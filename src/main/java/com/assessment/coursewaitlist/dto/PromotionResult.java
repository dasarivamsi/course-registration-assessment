package com.assessment.coursewaitlist.dto;

import java.util.List;

public record PromotionResult(
        List<String> promotedLearnerIds,
        List<String> skippedLearnerIds
) {
    public PromotionResult {
        promotedLearnerIds = List.copyOf(promotedLearnerIds);
        skippedLearnerIds = List.copyOf(skippedLearnerIds);
    }
}
