package com.assessment.coursewaitlist.dto;

import java.util.List;

public record DropResult(
        String droppedLearnerId,
        List<String> promotedLearnerIds,
        List<String> skippedLearnerIds
) {
    public DropResult {
        promotedLearnerIds = List.copyOf(promotedLearnerIds);
        skippedLearnerIds = List.copyOf(skippedLearnerIds);
    }
}
