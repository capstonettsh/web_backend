package com.communication.communication_backend.service.overallFeedback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class OverallFeedbackKafkaTopicNameFactoryTest {
    @Test
    void testCreate() {
        // Arrange
        OverallFeedbackKafkaTopicNameFactory factory = new OverallFeedbackKafkaTopicNameFactory();
        String sessionDateTime = "2025-02-12T10:00:00";
        String userId = "user123";

        // Act
        OverallFeedbackKafkaTopicName result = factory.create(sessionDateTime, userId);

        // Assert
        assertNotNull(result);
        // assertEquals(sessionDateTime, result.getSessionDateTime());
        // assertEquals(userId, result.getUserId());
    }
}
