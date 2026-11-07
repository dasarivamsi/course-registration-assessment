package com.assessment.coursewaitlist.repository;

import com.assessment.coursewaitlist.model.CourseOffering;
import com.assessment.coursewaitlist.model.Learner;
import com.assessment.coursewaitlist.model.RegistrationStateSnapshot;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class InMemoryRegistrationRepository implements RegistrationRepository {

    private final Map<String, CourseOffering> offerings = new HashMap<>();
    private final Map<String, Learner> learners = new HashMap<>();
    private final Map<String, Set<String>> enrollments = new HashMap<>();
    private final Map<String, Deque<String>> waitlists = new HashMap<>();
    private final Map<String, Set<String>> waitlistMembership = new HashMap<>();

    @Override
    public Optional<CourseOffering> findOffering(String offeringId) {
        return Optional.ofNullable(offerings.get(offeringId));
    }

    @Override
    public void saveOffering(CourseOffering offering) {
        offerings.put(offering.offeringId(), offering);
        enrollments.computeIfAbsent(offering.offeringId(), ignored -> new HashSet<>());
        waitlists.computeIfAbsent(offering.offeringId(), ignored -> new ArrayDeque<>());
        waitlistMembership.computeIfAbsent(offering.offeringId(), ignored -> new HashSet<>());
    }

    @Override
    public Optional<Learner> findLearner(String learnerId) {
        return Optional.ofNullable(learners.get(learnerId));
    }

    @Override
    public void saveLearner(Learner learner) {
        learners.put(learner.learnerId(), learner);
    }

    @Override
    public boolean isEnrolled(String offeringId, String learnerId) {
        return enrollmentSet(offeringId).contains(learnerId);
    }

    @Override
    public boolean isWaitlisted(String offeringId, String learnerId) {
        return waitlistSet(offeringId).contains(learnerId);
    }

    @Override
    public int enrollmentCount(String offeringId) {
        return enrollmentSet(offeringId).size();
    }

    @Override
    public void addEnrollment(String offeringId, String learnerId) {
        if (!enrollmentSet(offeringId).add(learnerId)) {
            throw new IllegalStateException("Learner is already enrolled");
        }
    }

    @Override
    public boolean removeEnrollment(String offeringId, String learnerId) {
        return enrollmentSet(offeringId).remove(learnerId);
    }

    @Override
    public int enqueue(String offeringId, String learnerId) {
        Set<String> membership = waitlistSet(offeringId);
        if (!membership.add(learnerId)) {
            throw new IllegalStateException("Learner is already waitlisted");
        }
        Deque<String> waitlist = waitlist(offeringId);
        waitlist.addLast(learnerId);
        return waitlist.size();
    }

    @Override
    public Optional<String> pollWaitlist(String offeringId) {
        String learnerId = waitlist(offeringId).pollFirst();
        if (learnerId != null) {
            waitlistSet(offeringId).remove(learnerId);
        }
        return Optional.ofNullable(learnerId);
    }

    @Override
    public RegistrationStateSnapshot snapshot(String offeringId) {
        return new RegistrationStateSnapshot(
                enrollmentSet(offeringId),
                new ArrayList<>(waitlist(offeringId))
        );
    }

    private Set<String> enrollmentSet(String offeringId) {
        return enrollments.computeIfAbsent(offeringId, ignored -> new HashSet<>());
    }

    private Deque<String> waitlist(String offeringId) {
        return waitlists.computeIfAbsent(offeringId, ignored -> new ArrayDeque<>());
    }

    private Set<String> waitlistSet(String offeringId) {
        return waitlistMembership.computeIfAbsent(offeringId, ignored -> new HashSet<>());
    }
}
