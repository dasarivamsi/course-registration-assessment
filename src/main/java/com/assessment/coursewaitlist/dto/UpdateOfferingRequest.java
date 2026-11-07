package com.assessment.coursewaitlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record UpdateOfferingRequest(
        @NotBlank String courseId,
        @Positive int capacity,
        @NotNull Set<@NotBlank String> prerequisiteCourseIds
) {
    public UpdateOfferingRequest {
        prerequisiteCourseIds = prerequisiteCourseIds == null
                ? null
                : Set.copyOf(prerequisiteCourseIds);
    }
}
