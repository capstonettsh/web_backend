package com.communication.communication_backend.service.facialAnalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FacialAnalysisKafkaTopicNameFactoryTest {
    private FacialAnalysisKafkaTopicNameFactory factory;

    @BeforeEach
    void setUp() {
        factory = new FacialAnalysisKafkaTopicNameFactory();
    }

    @Test
    void testCreate() {
        // Test data
        String sessionDateTime = "2025-02-09T10:00:00";
        String userId = "user123";

        // Create the object using the factory
        FacialAnalysisKafkaTopicName result = factory.create(sessionDateTime, userId);

        // Expected base value for comparison
        String expectedBase = userId + "_" + sessionDateTime + "_hume-face";

        // Verify the values are correctly initialized
        assertNotNull(result); // Ensure that the result is not null
        assertEquals(expectedBase, result.getHumeFace()); // Check the base topic name
        assertEquals(expectedBase + "-ranked", result.getHumeFaceRanked()); // Check the ranked topic name
        assertEquals(expectedBase + "-gpt-response", result.getHumeFaceGPTResponse()); // Check the GPT response topic name
    }
}
