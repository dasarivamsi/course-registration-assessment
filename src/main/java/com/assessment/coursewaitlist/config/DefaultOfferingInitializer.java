package com.assessment.coursewaitlist.config;

import com.assessment.coursewaitlist.model.CourseOffering;
import com.assessment.coursewaitlist.service.OfferingInitializationUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultOfferingInitializer implements ApplicationRunner {

    private final OfferingInitializationUseCase initializationUseCase;
    private final DefaultOfferingProperties properties;

    public DefaultOfferingInitializer(
            OfferingInitializationUseCase initializationUseCase,
            DefaultOfferingProperties properties
    ) {
        this.initializationUseCase = initializationUseCase;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        initializationUseCase.initializeDefaultOffering(new CourseOffering(
                properties.offeringId(),
                properties.courseId(),
                properties.capacity(),
                properties.prerequisiteCourseIds()
        ));
    }
}
