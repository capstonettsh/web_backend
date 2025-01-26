package com.communication.communication_backend.service.overallFeedback;

public class OverallFeedbackKafkaTopicName {
    private final String sessionDateTime;
    private final String userId;

    public OverallFeedbackKafkaTopicName(String sessionDateTime, String userId) {
        this.sessionDateTime = sessionDateTime;
        this.userId = userId;
    }

    private String getBase() {
        return userId + "_" + sessionDateTime;
    }

    public synchronized String getCombined() {
        return getBase() + "_combined";
    }

    public synchronized String getOverallFeedback() {
        return getBase() + "_overall_feedback";
    }
}
