package com.communication.communication_backend.service.toneAnalysis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToneAnalysisKafkaTopicName {
    private String sessionDateTime;
    private String userId;

    public void setSessionDateTime(String sessionDateTime) {
        this.sessionDateTime = sessionDateTime;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    private String getBase() {
        return userId + "_" + sessionDateTime + "_hume-speech";
    }

    @Bean
    public String getHumeSpeech() {
        return getBase();
    }

    @Bean
    public String getHumeSpeechShortened() {
        return getBase() + "-shortened";
    }

    @Bean
    public String getHumeSpeechExchange() {
        return getBase() + "-exchange";
    }

    @Bean
    public String getHumeSpeechGptResponse() {
        return getBase() + "-gpt-response";
    }
}
