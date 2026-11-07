package com.assessment.coursewaitlist.model;

import java.util.List;
import java.util.Set;

public record RegistrationStateSnapshot(
        Set<String> enrolledLearnerIds,
        List<String> waitlist
) {
    public RegistrationStateSnapshot {
        enrolledLearnerIds = Set.copyOf(enrolledLearnerIds);
        waitlist = List.copyOf(waitlist);
    }
}
