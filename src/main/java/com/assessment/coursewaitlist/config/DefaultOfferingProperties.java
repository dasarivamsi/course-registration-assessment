package com.assessment.coursewaitlist.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "course-waitlist.default-offering")
public record DefaultOfferingProperties(
        @NotBlank String offeringId,
        @NotBlank String courseId,
        @Positive int capacity,
        @NotNull Set<@NotBlank String> prerequisiteCourseIds
) {
    public DefaultOfferingProperties {
        prerequisiteCourseIds = prerequisiteCourseIds == null
                ? null
                : Set.copyOf(prerequisiteCourseIds);
    }
}
