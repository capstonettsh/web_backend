package com.communication.communication_backend.service.facialAnalysis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class FacialAnalysisConfig {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;

    public FacialAnalysisConfig(KafkaTemplate<String, String> kafkaTemplate,
                                ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
    }

    @Bean
    @Scope("prototype")
    public FacialRawConsumer facialRawConsumer(FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName) {
        return new FacialRawConsumer(facialAnalysisKafkaTopicName, kafkaTemplate, consumerFactory);
    }

    @Bean
    @Scope("prototype")
    public FacialRankedConsumer facialRankedConsumer(FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName) {
        return new FacialRankedConsumer(facialAnalysisKafkaTopicName, kafkaTemplate, consumerFactory);
    }
}
