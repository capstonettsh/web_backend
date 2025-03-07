package com.communication.communication_backend.service.toneAnalysis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.overallFeedback.ExchangesandFacialConsumer;
import com.communication.communication_backend.service.overallFeedback.GptResponseConsumer;

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
    public ExchangesandFacialConsumer exchangesConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                                               FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName, // Add this argument
                                               KafkaTemplate<String, String> kafkaTemplate,
                                               ConsumerFactory<String, String> consumerFactory,
                                               GptResponseConsumer gptResponseConsumer) {
        return new ExchangesandFacialConsumer(toneAnalysisKafkaTopicName, facialAnalysisKafkaTopicName, kafkaTemplate, consumerFactory, gptResponseConsumer);
    }

    @Bean
    @Scope("prototype")
    public HumeAiChatReader humeAiChatReader(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName) {
        return new HumeAiChatReader(kafkaTemplate, toneAnalysisKafkaTopicName);
    }
}
