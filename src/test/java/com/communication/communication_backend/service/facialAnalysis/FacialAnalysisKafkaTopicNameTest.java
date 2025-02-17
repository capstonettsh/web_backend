package com.communication.communication_backend.service.facialAnalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FacialAnalysisKafkaTopicNameTest {
    private FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;

    @BeforeEach
    void setUp() {
        String sessionDateTime = "2025-02-09T10:00:00";
        String userId = "user123";
        facialAnalysisKafkaTopicName = new FacialAnalysisKafkaTopicName(sessionDateTime, userId);
    }

    @Test
    void testGetHumeFace() {
        String expected = "user123_2025-02-09T10:00:00_hume-face";
        assertEquals(expected, facialAnalysisKafkaTopicName.getHumeFace());
    }

    @Test
    void testGetHumeFaceRanked() {
        String expected = "user123_2025-02-09T10:00:00_hume-face-ranked";
        assertEquals(expected, facialAnalysisKafkaTopicName.getHumeFaceRanked());
    }

    @Test
    void testGetHumeFaceGPTResponse() {
        String expected = "user123_2025-02-09T10:00:00_hume-face-gpt-response";
        assertEquals(expected, facialAnalysisKafkaTopicName.getHumeFaceGPTResponse());
    }
}
