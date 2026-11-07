package com.assessment.coursewaitlist.exception;

public class RegistrationException extends RuntimeException {

    private final ErrorCode code;

    public RegistrationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
