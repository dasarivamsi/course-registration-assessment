package com.assessment.coursewaitlist.dto;

public record EnrollmentResult(
        EnrollmentOutcome outcome,
        String learnerId,
        Integer waitlistPosition
) {
}
