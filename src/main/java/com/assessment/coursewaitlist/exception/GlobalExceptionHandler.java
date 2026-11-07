package com.assessment.coursewaitlist.exception;

import com.assessment.coursewaitlist.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ApiError> handleRegistrationException(
            RegistrationException exception
    ) {
        HttpStatus status = switch (exception.code()) {
            case OFFERING_NOT_FOUND, LEARNER_NOT_FOUND, NOT_ENROLLED ->
                    HttpStatus.NOT_FOUND;
            case INELIGIBLE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case ALREADY_ENROLLED, ALREADY_WAITLISTED, INVALID_CAPACITY ->
                    HttpStatus.CONFLICT;
        };
        LOGGER.warn(
                "Registration request rejected: code={}, status={}",
                exception.code(),
                status.value()
        );
        return ResponseEntity.status(status).body(new ApiError(
                exception.code().name(),
                exception.getMessage(),
                Instant.now()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        LOGGER.warn("Request validation failed");
        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        LOGGER.warn("Request body was missing or malformed");
        return badRequest("Request body is missing or malformed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        LOGGER.warn("Invalid request argument: {}", exception.getMessage());
        return badRequest(exception.getMessage());
    }

    private ResponseEntity<ApiError> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ApiError(
                "INVALID_REQUEST",
                message,
                Instant.now()
        ));
    }
}
