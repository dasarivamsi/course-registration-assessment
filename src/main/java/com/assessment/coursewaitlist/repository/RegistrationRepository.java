package com.assessment.coursewaitlist.repository;

import com.assessment.coursewaitlist.model.CourseOffering;
import com.assessment.coursewaitlist.model.Learner;
import com.assessment.coursewaitlist.model.RegistrationStateSnapshot;

import java.util.Optional;

public interface RegistrationRepository {

    Optional<CourseOffering> findOffering(String offeringId);

    void saveOffering(CourseOffering offering);

    Optional<Learner> findLearner(String learnerId);

    void saveLearner(Learner learner);

    boolean isEnrolled(String offeringId, String learnerId);

    boolean isWaitlisted(String offeringId, String learnerId);

    int enrollmentCount(String offeringId);

    void addEnrollment(String offeringId, String learnerId);

    boolean removeEnrollment(String offeringId, String learnerId);

    int enqueue(String offeringId, String learnerId);

    Optional<String> pollWaitlist(String offeringId);

    RegistrationStateSnapshot snapshot(String offeringId);
}
