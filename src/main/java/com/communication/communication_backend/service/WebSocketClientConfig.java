package com.communication.communication_backend.service;

import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.facialAnalysis.HumeAIExpressionManagementWebSocketClient;
import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

@Configuration
public class WebSocketClientConfig {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public WebSocketClientConfig(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    @Scope("prototype")
    public HumeAIAudioWebSocketClient humeAIWebSocketClient(URI serverUri, WebSocketSession frontendSession,
                                                            ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName) {
        return new HumeAIAudioWebSocketClient(serverUri, frontendSession, kafkaTemplate, toneAnalysisKafkaTopicName);
    }

    @Bean
    @Scope("prototype")
    public HumeAIExpressionManagementWebSocketClient humeAIExpressionManagementWebSocketClient(URI serverUri, WebSocketSession frontendSession,
                                                                                               FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName) {
        return new HumeAIExpressionManagementWebSocketClient(serverUri, frontendSession, kafkaTemplate, facialAnalysisKafkaTopicName);
    }
}