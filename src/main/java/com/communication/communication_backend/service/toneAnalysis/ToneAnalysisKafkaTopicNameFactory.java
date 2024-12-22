package com.communication.communication_backend.service.toneAnalysis;

import org.springframework.stereotype.Component;

@Component
public class ToneAnalysisKafkaTopicNameFactory {
    public ToneAnalysisKafkaTopicName create(String sessionDateTime, String userId) {
        return new ToneAnalysisKafkaTopicName(sessionDateTime, userId);
    }
}
