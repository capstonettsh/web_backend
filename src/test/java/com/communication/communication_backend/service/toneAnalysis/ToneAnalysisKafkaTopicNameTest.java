package com.communication.communication_backend.service.toneAnalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToneAnalysisKafkaTopicNameTest {

    private ToneAnalysisKafkaTopicName topicName;
    private final String sessionDateTime = "2024-02-17T12:00:00";
    private final String userId = "user123";

    @BeforeEach
    void setUp() {
        topicName = new ToneAnalysisKafkaTopicName(sessionDateTime, userId);
    }

    @Test
    void testGetHumeSpeech() {
        String expected = userId + "_" + sessionDateTime + "_hume-speech";
        assertEquals(expected, topicName.getHumeSpeech());
    }

    @Test
    void testGetHumeSpeechShortened() {
        String expected = userId + "_" + sessionDateTime + "_hume-speech-shortened";
        assertEquals(expected, topicName.getHumeSpeechShortened());
    }

    @Test
    void testGetHumeSpeechExchange() {
        String expected = userId + "_" + sessionDateTime + "_hume-speech-exchange";
        assertEquals(expected, topicName.getHumeSpeechExchange());
    }

    @Test
    void testGetHumeSpeechGptResponse() {
        String expected = userId + "_" + sessionDateTime + "_hume-speech-gpt-response";
        assertEquals(expected, topicName.getHumeSpeechGptResponse());
    }

    @Test
    void testGetOverallFeedback() {
        String expected = userId + "_" + sessionDateTime + "_hume-speech-overall-feedback";
        assertEquals(expected, topicName.getOverallFeedback());
    }
}
