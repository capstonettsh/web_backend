package com.communication.communication_backend.service.toneAnalysis;

import com.communication.communication_backend.service.HumeAIWebSocketClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

@Configuration
public class ToneAnalysisConfig {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;

    public ToneAnalysisConfig(KafkaTemplate<String, String> kafkaTemplate,
                              ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
    }

    @Bean
    @Scope("prototype")
    public RawConsumer rawConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName) {
        return new RawConsumer(toneAnalysisKafkaTopicName, kafkaTemplate, consumerFactory);
    }

    @Bean
    @Scope("prototype")
    public ShortenedConsumer shortenedConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName) {
        return new ShortenedConsumer(toneAnalysisKafkaTopicName, kafkaTemplate, consumerFactory);
    }

    @Bean
    @Scope("prototype")
    public ExchangesConsumer exchangesConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName) {
        return new ExchangesConsumer(toneAnalysisKafkaTopicName, kafkaTemplate, consumerFactory);
    }
}
