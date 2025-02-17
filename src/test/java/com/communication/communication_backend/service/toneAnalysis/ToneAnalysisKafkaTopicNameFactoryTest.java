package com.communication.communication_backend.service.toneAnalysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToneAnalysisKafkaTopicNameFactoryTest {

    @Test
    void testCreate() {
        // Arrange
        ToneAnalysisKafkaTopicNameFactory factory = new ToneAnalysisKafkaTopicNameFactory();
        String sessionDateTime = "2024-02-17T12:00:00";
        String userId = "user123";

        // Act
        ToneAnalysisKafkaTopicName topicName = factory.create(sessionDateTime, userId);

        // Assert
        assertNotNull(topicName, "Factory should return a non-null instance.");
        assertEquals(userId + "_" + sessionDateTime + "_hume-speech", topicName.getHumeSpeech());
    }
}
