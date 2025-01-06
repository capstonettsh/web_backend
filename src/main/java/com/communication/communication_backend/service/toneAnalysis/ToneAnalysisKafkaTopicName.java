package com.communication.communication_backend.service.toneAnalysis;

public class ToneAnalysisKafkaTopicName {
    private final String sessionDateTime;
    private final String userId;

    public ToneAnalysisKafkaTopicName(String sessionDateTime, String userId) {
        this.sessionDateTime = sessionDateTime;
        this.userId = userId;
    }

    private String getBase() {
        return userId + "_" + sessionDateTime + "_hume-speech";
    }

    public synchronized String getHumeSpeech() {
        return getBase();
    }

    public synchronized String getHumeSpeechShortened() {
        return getBase() + "-shortened";
    }

    public synchronized String getHumeSpeechExchange() {
        return getBase() + "-exchange";
    }

    public synchronized String getHumeSpeechGptResponse() {
        return getBase() + "-gpt-response";
    }

    public synchronized String getOverallFeedback() {
        return getBase() + "-overall-feedback";
    }
}
