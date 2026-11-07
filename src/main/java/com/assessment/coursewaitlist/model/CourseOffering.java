package com.assessment.coursewaitlist.model;

import java.util.Objects;
import java.util.Set;

public record CourseOffering(
        String offeringId,
        String courseId,
        int capacity,
        Set<String> prerequisiteCourseIds
) {
    public CourseOffering {
        offeringId = requireText(offeringId, "offeringId");
        courseId = requireText(courseId, "courseId");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        Objects.requireNonNull(prerequisiteCourseIds, "prerequisiteCourseIds");
        if (prerequisiteCourseIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException(
                    "prerequisiteCourseIds must contain only non-blank values"
            );
        }
        prerequisiteCourseIds = Set.copyOf(prerequisiteCourseIds);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
