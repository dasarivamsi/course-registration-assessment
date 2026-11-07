package com.assessment.coursewaitlist.service;

import com.assessment.coursewaitlist.model.CourseOffering;
import com.assessment.coursewaitlist.model.Learner;

public interface EligibilityPolicy {

    boolean isEligible(Learner learner, CourseOffering offering);
}
