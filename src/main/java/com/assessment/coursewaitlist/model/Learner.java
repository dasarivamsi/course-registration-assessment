package com.assessment.coursewaitlist.model;

import java.util.Objects;
import java.util.Set;

public record Learner(String learnerId, Set<String> completedCourseIds) {

    public Learner {
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("learnerId must not be blank");
        }
        Objects.requireNonNull(completedCourseIds, "completedCourseIds");
        if (completedCourseIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException(
                    "completedCourseIds must contain only non-blank values"
            );
        }
        completedCourseIds = Set.copyOf(completedCourseIds);
    }
}
