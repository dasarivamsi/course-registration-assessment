package com.assessment.coursewaitlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CourseWaitlistApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseWaitlistApplication.class, args);
    }
}
