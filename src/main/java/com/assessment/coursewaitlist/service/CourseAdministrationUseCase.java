package com.assessment.coursewaitlist.service;

import com.assessment.coursewaitlist.dto.CourseUpdateResult;
import com.assessment.coursewaitlist.dto.UpdateOfferingRequest;

public interface CourseAdministrationUseCase {

    CourseUpdateResult updateOffering(String offeringId, UpdateOfferingRequest request);
}
