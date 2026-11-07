package com.assessment.coursewaitlist.service;

import com.assessment.coursewaitlist.model.CourseOffering;
import com.assessment.coursewaitlist.model.Learner;
import org.springframework.stereotype.Component;

@Component
public class AllPrerequisitesEligibilityPolicy implements EligibilityPolicy {

    @Override
    public boolean isEligible(Learner learner, CourseOffering offering) {
        return learner.completedCourseIds().containsAll(offering.prerequisiteCourseIds());
    }
}
