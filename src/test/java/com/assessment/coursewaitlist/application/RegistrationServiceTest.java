package com.assessment.coursewaitlist.service;

import com.assessment.coursewaitlist.dto.EnrollmentOutcome;
import com.assessment.coursewaitlist.dto.EnrollmentRequest;
import com.assessment.coursewaitlist.dto.EnrollmentResult;
import com.assessment.coursewaitlist.dto.UpdateOfferingRequest;
import com.assessment.coursewaitlist.exception.ErrorCode;
import com.assessment.coursewaitlist.exception.RegistrationException;
import com.assessment.coursewaitlist.model.CourseOffering;
import com.assessment.coursewaitlist.repository.InMemoryRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationServiceTest {

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(
                new InMemoryRegistrationRepository(),
                new AllPrerequisitesEligibilityPolicy()
        );
    }

    @Test
    void enrollsLearnerWhoCompletedEveryPrerequisite() {
        initialize(2, Set.of("P1", "P2"));

        var result = service.enroll(
                "offering-1",
                new EnrollmentRequest("learner-1", Set.of("P1", "P2", "P3"))
        );

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.ENROLLED);
        assertThat(service.getOffering("offering-1").enrolledLearnerIds())
                .containsExactly("learner-1");
    }

    @Test
    void rejectsLearnerMissingAnyPrerequisiteWithoutChangingState() {
        initialize(2, Set.of("P1", "P2"));

        assertCode(
                () -> service.enroll(
                        "offering-1",
                        new EnrollmentRequest("learner-1", Set.of("P1"))
                ),
                ErrorCode.INELIGIBLE
        );
        assertThat(service.getOffering("offering-1").enrolledLearnerIds()).isEmpty();
        assertThat(service.getOffering("offering-1").waitlist()).isEmpty();
    }

    @Test
    void offeringWithoutPrerequisitesAcceptsLearner() {
        initialize(1, Set.of());

        assertThat(service.enroll(
                "offering-1",
                new EnrollmentRequest("learner-1", Set.of())
        ).outcome()).isEqualTo(EnrollmentOutcome.ENROLLED);
    }

    @Test
    void fullOfferingWaitlistsEligibleLearnerOnce() {
        initialize(1, Set.of("P1"));
        enroll("learner-1", Set.of("P1"));

        var result = enroll("learner-2", Set.of("P1"));

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.WAITLISTED);
        assertThat(result.waitlistPosition()).isEqualTo(1);
        assertCode(
                () -> enroll("learner-2", Set.of("P1")),
                ErrorCode.ALREADY_WAITLISTED
        );
        assertThat(service.getOffering("offering-1").waitlist())
                .containsExactly("learner-2");
    }

    @Test
    void droppingLearnerPromotesInFifoOrder() {
        initialize(1, Set.of("P1"));
        enroll("learner-1", Set.of("P1"));
        enroll("learner-2", Set.of("P1"));
        enroll("learner-3", Set.of("P1"));

        var result = service.drop("offering-1", "learner-1");

        assertThat(result.promotedLearnerIds()).containsExactly("learner-2");
        assertThat(result.skippedLearnerIds()).isEmpty();
        assertThat(service.getOffering("offering-1").enrolledLearnerIds())
                .containsExactly("learner-2");
        assertThat(service.getOffering("offering-1").waitlist())
                .containsExactly("learner-3");
    }

    @Test
    void updatedPrerequisitesSkipIneligibleHeadAndPromoteNextLearner() {
        initialize(1, Set.of("P1"));
        enroll("enrolled", Set.of("P1"));
        enroll("learner-a", Set.of("P1"));
        enroll("learner-b", Set.of("P1", "P2"));
        service.updateOffering(
                "offering-1",
                new UpdateOfferingRequest("COURSE-1", 1, Set.of("P1", "P2"))
        );

        var result = service.drop("offering-1", "enrolled");

        assertThat(result.skippedLearnerIds()).containsExactly("learner-a");
        assertThat(result.promotedLearnerIds()).containsExactly("learner-b");
        assertThat(service.getOffering("offering-1").waitlist()).isEmpty();
    }

    @Test
    void leavesSeatVacantWhenAllWaitingLearnersBecomeIneligible() {
        initialize(1, Set.of("P1"));
        enroll("enrolled", Set.of("P1"));
        enroll("learner-a", Set.of("P1"));
        enroll("learner-b", Set.of("P1"));
        service.updateOffering(
                "offering-1",
                new UpdateOfferingRequest("COURSE-1", 1, Set.of("P1", "P2"))
        );

        var result = service.drop("offering-1", "enrolled");

        assertThat(result.skippedLearnerIds()).containsExactly("learner-a", "learner-b");
        assertThat(result.promotedLearnerIds()).isEmpty();
        assertThat(service.getOffering("offering-1").enrolledLearnerIds()).isEmpty();
    }

    @Test
    void invalidAndRepeatedDropsLeaveStateConsistent() {
        initialize(1, Set.of());
        enroll("learner-1", Set.of());

        assertCode(() -> service.drop("offering-1", "unknown"), ErrorCode.NOT_ENROLLED);
        assertThat(service.getOffering("offering-1").enrolledLearnerIds())
                .containsExactly("learner-1");

        service.drop("offering-1", "learner-1");
        assertCode(
                () -> service.drop("offering-1", "learner-1"),
                ErrorCode.NOT_ENROLLED
        );
    }

    @Test
    void increasingCapacityPromotesMultipleLearners() {
        initialize(1, Set.of());
        enroll("learner-1", Set.of());
        enroll("learner-2", Set.of());
        enroll("learner-3", Set.of());

        var result = service.updateOffering(
                "offering-1",
                new UpdateOfferingRequest("COURSE-1", 3, Set.of())
        );

        assertThat(result.promotedLearnerIds()).containsExactly("learner-2", "learner-3");
        assertThat(result.offering().enrolledLearnerIds()).hasSize(3);
        assertThat(result.offering().waitlist()).isEmpty();
    }

    @Test
    void rejectsCapacityBelowCurrentEnrollment() {
        initialize(2, Set.of());
        enroll("learner-1", Set.of());
        enroll("learner-2", Set.of());

        assertCode(
                () -> service.updateOffering(
                        "offering-1",
                        new UpdateOfferingRequest("COURSE-1", 1, Set.of())
                ),
                ErrorCode.INVALID_CAPACITY
        );
        assertThat(service.getOffering("offering-1").offering().capacity()).isEqualTo(2);
    }

    @Test
    void concurrentLearnersCompeteSafelyForFinalSeat() throws Exception {
        initialize(1, Set.of());
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<EnrollmentOutcome> first = executor.submit(() -> {
                start.await();
                return enroll("learner-1", Set.of()).outcome();
            });
            Future<EnrollmentOutcome> second = executor.submit(() -> {
                start.await();
                return enroll("learner-2", Set.of()).outcome();
            });
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(
                            EnrollmentOutcome.ENROLLED,
                            EnrollmentOutcome.WAITLISTED
                    );
        }

        assertThat(service.getOffering("offering-1").enrolledLearnerIds()).hasSize(1);
        assertThat(service.getOffering("offering-1").waitlist()).hasSize(1);
    }

    @Test
    void concurrentDuplicateEnrollmentCreatesOneRecord() throws Exception {
        initialize(2, Set.of());
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(() -> enrollAndReturnCode(start));
            Future<String> second = executor.submit(() -> enrollAndReturnCode(start));
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder("ENROLLED", "ALREADY_ENROLLED");
        }

        assertThat(service.getOffering("offering-1").enrolledLearnerIds())
                .containsExactly("learner-1");
    }

    private String enrollAndReturnCode(CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            return enroll("learner-1", Set.of()).outcome().name();
        } catch (RegistrationException exception) {
            return exception.code().name();
        }
    }

    private EnrollmentResult enroll(
            String learnerId,
            Set<String> completedCourseIds
    ) {
        return service.enroll(
                "offering-1",
                new EnrollmentRequest(learnerId, completedCourseIds)
        );
    }

    private void initialize(int capacity, Set<String> prerequisites) {
        service.initializeDefaultOffering(
                new CourseOffering("offering-1", "COURSE-1", capacity, prerequisites)
        );
    }

    private void assertCode(Runnable operation, ErrorCode code) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(RegistrationException.class)
                .extracting(exception -> ((RegistrationException) exception).code())
                .isEqualTo(code);
    }
}
