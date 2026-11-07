package com.assessment.coursewaitlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record EnrollmentRequest(
        @NotBlank String learnerId,
        @NotNull Set<@NotBlank String> completedCourseIds
) {
    public EnrollmentRequest {
        completedCourseIds = completedCourseIds == null
                ? null
                : Set.copyOf(completedCourseIds);
    }
}
