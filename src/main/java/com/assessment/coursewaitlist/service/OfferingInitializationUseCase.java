package com.assessment.coursewaitlist.service;

import com.assessment.coursewaitlist.model.CourseOffering;

public interface OfferingInitializationUseCase {

    void initializeDefaultOffering(CourseOffering offering);
}
