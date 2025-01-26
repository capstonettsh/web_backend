package com.communication.communication_backend.service.facialAnalysis;

public class FacialAnalysisKafkaTopicName {
    private final String sessionDateTime;
    private final String userId;

    public FacialAnalysisKafkaTopicName(String sessionDateTime, String userId) {
        this.sessionDateTime = sessionDateTime;
        this.userId = userId;
    }

    private String getBase() {
        return userId + "_" + sessionDateTime + "_hume-face";
    }

    public synchronized String getHumeFace() {
        return getBase();
    }

    public synchronized String getHumeFaceRanked() {
        return getBase() + "-ranked";
    }

    public synchronized String getHumeFaceGPTResponse() {
        return getBase() + "-gpt-response";
    }
}
