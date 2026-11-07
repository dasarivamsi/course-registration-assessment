package com.assessment.coursewaitlist.dto;

import java.util.List;

public record CourseUpdateResult(
        OfferingSnapshot offering,
        List<String> promotedLearnerIds,
        List<String> skippedLearnerIds
) {
    public CourseUpdateResult {
        promotedLearnerIds = List.copyOf(promotedLearnerIds);
        skippedLearnerIds = List.copyOf(skippedLearnerIds);
    }
}
