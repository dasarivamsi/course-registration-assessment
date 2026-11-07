package com.assessment.coursewaitlist.dto;

import com.assessment.coursewaitlist.model.CourseOffering;

import java.util.List;
import java.util.Set;

public record OfferingSnapshot(
        CourseOffering offering,
        Set<String> enrolledLearnerIds,
        List<String> waitlist
) {
    public OfferingSnapshot {
        enrolledLearnerIds = Set.copyOf(enrolledLearnerIds);
        waitlist = List.copyOf(waitlist);
    }
}
