package com.assessment.coursewaitlist.service;

import com.assessment.coursewaitlist.dto.CourseUpdateResult;
import com.assessment.coursewaitlist.dto.DropResult;
import com.assessment.coursewaitlist.dto.EnrollmentOutcome;
import com.assessment.coursewaitlist.dto.EnrollmentRequest;
import com.assessment.coursewaitlist.dto.EnrollmentResult;
import com.assessment.coursewaitlist.dto.OfferingSnapshot;
import com.assessment.coursewaitlist.dto.PromotionResult;
import com.assessment.coursewaitlist.dto.UpdateOfferingRequest;
import com.assessment.coursewaitlist.exception.RegistrationException;
import com.assessment.coursewaitlist.model.CourseOffering;
import com.assessment.coursewaitlist.model.Learner;
import com.assessment.coursewaitlist.model.RegistrationStateSnapshot;
import com.assessment.coursewaitlist.repository.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static com.assessment.coursewaitlist.exception.ErrorCode.ALREADY_ENROLLED;
import static com.assessment.coursewaitlist.exception.ErrorCode.ALREADY_WAITLISTED;
import static com.assessment.coursewaitlist.exception.ErrorCode.INELIGIBLE;
import static com.assessment.coursewaitlist.exception.ErrorCode.INVALID_CAPACITY;
import static com.assessment.coursewaitlist.exception.ErrorCode.NOT_ENROLLED;
import static com.assessment.coursewaitlist.exception.ErrorCode.OFFERING_NOT_FOUND;

@Service
public class RegistrationService implements
        RegistrationUseCase,
        CourseAdministrationUseCase,
        OfferingInitializationUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);

    private final ReentrantLock operationLock = new ReentrantLock(true);
    private final RegistrationRepository repository;
    private final EligibilityPolicy eligibilityPolicy;

    public RegistrationService(
            RegistrationRepository repository,
            EligibilityPolicy eligibilityPolicy
    ) { 
        this.repository = repository;
        this.eligibilityPolicy = eligibilityPolicy;
    }

    @Override
    public void initializeDefaultOffering(CourseOffering offering) {
        operationLock.lock();
        try {
            if (repository.findOffering(offering.offeringId()).isEmpty()) {
                repository.saveOffering(offering);
                LOGGER.info(
                        "Default offering initialized: offeringId={}, courseId={}, capacity={}",
                        offering.offeringId(),
                        offering.courseId(),
                        offering.capacity()
                );
            } else {
                LOGGER.debug(
                        "Default offering already initialized: offeringId={}",
                        offering.offeringId()
                );
            }
        } finally {
            operationLock.unlock();
        }
    }

    @Override
    public EnrollmentResult enroll(String offeringId, EnrollmentRequest request) {
        operationLock.lock();
        try {
            CourseOffering offering = requireOffering(offeringId);
            if (repository.isEnrolled(offeringId, request.learnerId())) {
                throw new RegistrationException(ALREADY_ENROLLED, "Learner is already enrolled");
            }
            if (repository.isWaitlisted(offeringId, request.learnerId())) {
                throw new RegistrationException(
                        ALREADY_WAITLISTED,
                        "Learner is already waitlisted"
                );
            }

            Learner learner = new Learner(request.learnerId(), request.completedCourseIds());
            if (!eligibilityPolicy.isEligible(learner, offering)) {
                LOGGER.info(
                        "Enrollment rejected as ineligible: offeringId={}, learnerId={}",
                        offeringId,
                        learner.learnerId()
                );
                throw new RegistrationException(
                        INELIGIBLE,
                        "Learner has not completed every required prerequisite"
                );
            }

            repository.saveLearner(learner);
            if (hasAvailableSeat(offering)) {
                repository.addEnrollment(offeringId, learner.learnerId());
                LOGGER.info(
                        "Learner enrolled: offeringId={}, learnerId={}, enrollmentCount={}",
                        offeringId,
                        learner.learnerId(),
                        repository.enrollmentCount(offeringId)
                );
                return new EnrollmentResult(
                        EnrollmentOutcome.ENROLLED,
                        learner.learnerId(),
                        null
                );
            }

            int position = repository.enqueue(offeringId, learner.learnerId());
            LOGGER.info(
                    "Learner waitlisted: offeringId={}, learnerId={}, position={}",
                    offeringId,
                    learner.learnerId(),
                    position
            );
            return new EnrollmentResult(
                    EnrollmentOutcome.WAITLISTED,
                    learner.learnerId(),
                    position
            );
        } finally {
            operationLock.unlock();
        }
    }

    @Override
    public DropResult drop(String offeringId, String learnerId) {
        operationLock.lock();
        try {
            CourseOffering offering = requireOffering(offeringId);
            if (!repository.removeEnrollment(offeringId, learnerId)) {
                throw new RegistrationException(NOT_ENROLLED, "Learner is not enrolled");
            }
            PromotionResult promotion = promoteEligibleLearners(offering);
            LOGGER.info(
                    "Learner dropped: offeringId={}, learnerId={}, promotedCount={}, skippedCount={}",
                    offeringId,
                    learnerId,
                    promotion.promotedLearnerIds().size(),
                    promotion.skippedLearnerIds().size()
            );
            return new DropResult(
                    learnerId,
                    promotion.promotedLearnerIds(),
                    promotion.skippedLearnerIds()
            );
        } finally {
            operationLock.unlock();
        }
    }

    @Override
    public OfferingSnapshot getOffering(String offeringId) {
        operationLock.lock();
        try {
            CourseOffering offering = requireOffering(offeringId);
            return snapshot(offering);
        } finally {
            operationLock.unlock();
        }
    }

    @Override
    public CourseUpdateResult updateOffering(
            String offeringId,
            UpdateOfferingRequest request
    ) {
        operationLock.lock();
        try {
            CourseOffering current = requireOffering(offeringId);
            if (request.capacity() < repository.enrollmentCount(offeringId)) {
                throw new RegistrationException(
                        INVALID_CAPACITY,
                        "Capacity cannot be lower than current enrollment"
                );
            }

            CourseOffering updated = new CourseOffering(
                    offeringId,
                    request.courseId(),
                    request.capacity(),
                    request.prerequisiteCourseIds()
            );
            repository.saveOffering(updated);
            PromotionResult promotion = promoteEligibleLearners(updated);
            LOGGER.info(
                    "Offering updated: offeringId={}, oldCapacity={}, newCapacity={}, "
                            + "prerequisiteCount={}, promotedCount={}, skippedCount={}",
                    offeringId,
                    current.capacity(),
                    updated.capacity(),
                    updated.prerequisiteCourseIds().size(),
                    promotion.promotedLearnerIds().size(),
                    promotion.skippedLearnerIds().size()
            );
            return new CourseUpdateResult(
                    snapshot(updated),
                    promotion.promotedLearnerIds(),
                    promotion.skippedLearnerIds()
            );
        } finally {
            operationLock.unlock();
        }
    }

    private PromotionResult promoteEligibleLearners(CourseOffering offering) {
        List<String> promoted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        while (hasAvailableSeat(offering)) {
            String learnerId = repository.pollWaitlist(offering.offeringId()).orElse(null);
            if (learnerId == null) {
                break;
            }
            Learner learner = repository.findLearner(learnerId).orElse(null);
            if (learner == null || !eligibilityPolicy.isEligible(learner, offering)) {
                skipped.add(learnerId);
                LOGGER.info(
                        "Waitlisted learner skipped: offeringId={}, learnerId={}",
                        offering.offeringId(),
                        learnerId
                );
                continue;
            }
            repository.addEnrollment(offering.offeringId(), learnerId);
            promoted.add(learnerId);
            LOGGER.info(
                    "Waitlisted learner promoted: offeringId={}, learnerId={}",
                    offering.offeringId(),
                    learnerId
            );
        }
        return new PromotionResult(promoted, skipped);
    }

    private boolean hasAvailableSeat(CourseOffering offering) {
        return repository.enrollmentCount(offering.offeringId()) < offering.capacity();
    }

    private OfferingSnapshot snapshot(CourseOffering offering) {
        RegistrationStateSnapshot state = repository.snapshot(offering.offeringId());
        return new OfferingSnapshot(
                offering,
                state.enrolledLearnerIds(),
                state.waitlist()
        );
    }

    private CourseOffering requireOffering(String offeringId) {
        return repository.findOffering(offeringId).orElseThrow(() ->
                new RegistrationException(OFFERING_NOT_FOUND, "Course offering was not found"));
    }
}
