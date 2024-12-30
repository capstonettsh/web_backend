package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

public class ExchangesConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    @Autowired
    private OpenAiClient openAiClient;

    public ExchangesConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ConsumerFactory<String, String> consumerFactory) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.container = createContainer();
        this.container.start();
    }

    private KafkaMessageListenerContainer<String, String> createContainer() {
        ContainerProperties containerProperties = new ContainerProperties(toneAnalysisKafkaTopicName.getHumeSpeechExchange());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consume(record.value());
            }
        });
        containerProperties.setGroupId("exchange-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consume(String exchangeJson) {
        try {
            // exchangeJson is a JSON representation of the user-assistant exchange
            System.out.println("Received exchange for ChatGPT processing: " + exchangeJson);

            // Get empathy rating from ChatGPT
            String rating = openAiClient.getEmpathyRating(exchangeJson);
            System.out.println("check chat gpt response" + rating);

            // Append rating to the exchange JSON
            JsonNode exchangeNode = objectMapper.readTree(exchangeJson);
            // Convert to ObjectNode to add a field
            ObjectNode node = (ObjectNode) exchangeNode;
            node.put("rating", rating);

            String updatedExchange = objectMapper.writeValueAsString(node);
            System.out.println("check updated exchange " + updatedExchange);

            // Send updated exchange to GPT responses topic
            kafkaTemplate.send(toneAnalysisKafkaTopicName.getHumeSpeechGptResponse(), updatedExchange);

        } catch (Exception e) {
            System.err.println("Error processing exchange with ChatGPT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
