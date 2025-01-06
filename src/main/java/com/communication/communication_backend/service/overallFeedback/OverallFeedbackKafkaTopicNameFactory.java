package com.communication.communication_backend.service.overallFeedback;

import org.springframework.stereotype.Component;

@Component
public class OverallFeedbackKafkaTopicNameFactory {
    public OverallFeedbackKafkaTopicName create(String sessionDateTime, String userId) {
        return new OverallFeedbackKafkaTopicName(sessionDateTime, userId);
    }
}
