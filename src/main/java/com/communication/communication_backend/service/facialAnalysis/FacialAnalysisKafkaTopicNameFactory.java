package com.communication.communication_backend.service.facialAnalysis;

import org.springframework.stereotype.Component;

@Component
public class FacialAnalysisKafkaTopicNameFactory {
    public FacialAnalysisKafkaTopicName create(String sessionDateTime, String userId) {
        return new FacialAnalysisKafkaTopicName(sessionDateTime, userId);
    }
}
