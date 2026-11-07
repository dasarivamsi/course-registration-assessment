package com.assessment.coursewaitlist.controller;

import com.assessment.coursewaitlist.dto.CourseUpdateResult;
import com.assessment.coursewaitlist.dto.UpdateOfferingRequest;
import com.assessment.coursewaitlist.service.CourseAdministrationUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/offerings")
public class AdminController {

    private final CourseAdministrationUseCase administrationUseCase;

    public AdminController(CourseAdministrationUseCase administrationUseCase) {
        this.administrationUseCase = administrationUseCase;
    }

    @PutMapping("/{offeringId}")
    public CourseUpdateResult updateOffering(
            @PathVariable String offeringId,
            @Valid @RequestBody UpdateOfferingRequest request
    ) {
        return administrationUseCase.updateOffering(offeringId, request);
    }
}
