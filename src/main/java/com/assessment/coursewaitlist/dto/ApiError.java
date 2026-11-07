package com.assessment.coursewaitlist.dto;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp) {
}
