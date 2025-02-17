package com.communication.communication_backend.service.overallFeedback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OverallFeedbackKafkaTopicNameTest {
    @Test
    void testGetCombined() {
        // Arrange
        String sessionDateTime = "2025-02-12T10:00:00";
        String userId = "user123";
        OverallFeedbackKafkaTopicName topicName = new OverallFeedbackKafkaTopicName(sessionDateTime, userId);

        // Act
        String combinedTopic = topicName.getCombined();

        // Assert
        assertEquals("user123_2025-02-12T10:00:00_combined", combinedTopic);
    }

    @Test
    void testGetOverallFeedback() {
        // Arrange
        String sessionDateTime = "2025-02-12T10:00:00";
        String userId = "user123";
        OverallFeedbackKafkaTopicName topicName = new OverallFeedbackKafkaTopicName(sessionDateTime, userId);

        // Act
        String overallFeedbackTopic = topicName.getOverallFeedback();

        // Assert
        assertEquals("user123_2025-02-12T10:00:00_overall_feedback", overallFeedbackTopic);
    }
}
