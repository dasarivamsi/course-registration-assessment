package com.assessment.coursewaitlist.controller;

import com.assessment.coursewaitlist.dto.DropResult;
import com.assessment.coursewaitlist.dto.EnrollmentOutcome;
import com.assessment.coursewaitlist.dto.EnrollmentRequest;
import com.assessment.coursewaitlist.dto.EnrollmentResult;
import com.assessment.coursewaitlist.dto.OfferingSnapshot;
import com.assessment.coursewaitlist.service.RegistrationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/offerings/{offeringId}")
public class RegistrationController {

    private final RegistrationUseCase registrationUseCase;

    public RegistrationController(RegistrationUseCase registrationUseCase) {
        this.registrationUseCase = registrationUseCase;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResult> enroll(
            @PathVariable String offeringId,
            @Valid @RequestBody EnrollmentRequest request
    ) {
        EnrollmentResult result = registrationUseCase.enroll(offeringId, request);
        HttpStatus status = result.outcome() == EnrollmentOutcome.ENROLLED
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }

    @DeleteMapping("/enrollments/{learnerId}")
    public DropResult drop(
            @PathVariable String offeringId,
            @PathVariable String learnerId
    ) {
        return registrationUseCase.drop(offeringId, learnerId);
    }

    @GetMapping
    public OfferingSnapshot getOffering(@PathVariable String offeringId) {
        return registrationUseCase.getOffering(offeringId);
    }
}
