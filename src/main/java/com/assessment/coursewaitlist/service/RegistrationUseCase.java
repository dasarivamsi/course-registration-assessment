package com.assessment.coursewaitlist.service;

import com.assessment.coursewaitlist.dto.DropResult;
import com.assessment.coursewaitlist.dto.EnrollmentRequest;
import com.assessment.coursewaitlist.dto.EnrollmentResult;
import com.assessment.coursewaitlist.dto.OfferingSnapshot;

public interface RegistrationUseCase {

    EnrollmentResult enroll(String offeringId, EnrollmentRequest request);

    DropResult drop(String offeringId, String learnerId);

    OfferingSnapshot getOffering(String offeringId);
}
