package com.assessment.coursewaitlist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CourseWaitlistApiTest {

    private static final String OFFERING_ID = "CS201-FALL-2026";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loadsConfiguredDefaultOfferingAtStartup() throws Exception {
        mockMvc.perform(get("/api/v1/offerings/{offeringId}", OFFERING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offering.offeringId").value(OFFERING_ID))
                .andExpect(jsonPath("$.offering.courseId").value("CS201"))
                .andExpect(jsonPath("$.offering.capacity").value(2))
                .andExpect(jsonPath("$.offering.prerequisiteCourseIds.length()").value(2))
                .andExpect(jsonPath("$.enrolledLearnerIds").isEmpty())
                .andExpect(jsonPath("$.waitlist").isEmpty());
    }

    @Test
    void versionedApiEnrollsThenWaitlistsEligibleLearners() throws Exception {
        enroll("learner-1", "[\"CS101\",\"MATH101\"]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"));
        enroll("learner-2", "[\"CS101\",\"MATH101\"]")
                .andExpect(status().isCreated());
        enroll("learner-3", "[\"CS101\",\"MATH101\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));

        enroll("learner-3", "[\"CS101\",\"MATH101\"]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_WAITLISTED"));
    }

    @Test
    void mapsValidationEligibilityAndRepeatedDropErrors() throws Exception {
        enroll("", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        enroll("learner-1", "[\"CS101\"]")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INELIGIBLE"));

        enroll("learner-1", "[\"CS101\",\"MATH101\"]")
                .andExpect(status().isCreated());

        mockMvc.perform(delete(
                        "/api/v1/offerings/{offeringId}/enrollments/{learnerId}",
                        OFFERING_ID,
                        "learner-1"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.droppedLearnerId").value("learner-1"));

        mockMvc.perform(delete(
                        "/api/v1/offerings/{offeringId}/enrollments/{learnerId}",
                        OFFERING_ID,
                        "learner-1"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_ENROLLED"));
    }

    @Test
    void adminUpdatesDefaultOfferingAndNoCreateEndpointExists() throws Exception {
        mockMvc.perform(put("/api/v1/admin/offerings/{offeringId}", OFFERING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId":"CS201-UPDATED",
                                  "capacity":3,
                                  "prerequisiteCourseIds":["CS101"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offering.offering.courseId").value("CS201-UPDATED"))
                .andExpect(jsonPath("$.offering.offering.capacity").value(3));

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unversionedRegistrationPathIsNotExposed() throws Exception {
        mockMvc.perform(get("/api/offerings/{offeringId}", OFFERING_ID))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions enroll(
            String learnerId,
            String completedCourseIdsJson
    ) throws Exception {
        return mockMvc.perform(post(
                        "/api/v1/offerings/{offeringId}/enrollments",
                        OFFERING_ID
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "learnerId":"%s",
                          "completedCourseIds":%s
                        }
                        """.formatted(learnerId, completedCourseIdsJson)));
    }
}
