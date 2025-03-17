package com.communication.communication_backend.service.overallFeedback;

import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class OverallFeedbackConfig {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;

    public OverallFeedbackConfig(KafkaTemplate<String, String> kafkaTemplate,
                                 ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
    }

    @Bean
    @Scope("prototype")
    public ExchangesandFacialConsumer exchangesandfacialConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                                               FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName, // Add this argument
                                               KafkaTemplate<String, String> kafkaTemplate,
                                               ConsumerFactory<String, String> consumerFactory,
                                               GptResponseConsumer gptResponseConsumer) {
        return new ExchangesandFacialConsumer(toneAnalysisKafkaTopicName, facialAnalysisKafkaTopicName, kafkaTemplate, consumerFactory, gptResponseConsumer);
    }

    @Bean
    @Scope("prototype")
    public GptResponseConsumer gptResponseConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName, FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName, OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName) {
        return new GptResponseConsumer(toneAnalysisKafkaTopicName, facialAnalysisKafkaTopicName, overallFeedbackKafkaTopicName, kafkaTemplate, consumerFactory);
    }

    @Bean
    @Scope("prototype")
    public OverallFeedbackExchangesConsumer overallFeedbackExchangesConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName, OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName) {
        return new OverallFeedbackExchangesConsumer(toneAnalysisKafkaTopicName, overallFeedbackKafkaTopicName, kafkaTemplate, consumerFactory);
    }
}
