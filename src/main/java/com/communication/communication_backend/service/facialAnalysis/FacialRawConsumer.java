package com.communication.communication_backend.service.facialAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FacialRawConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;

    public FacialRawConsumer(FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName, KafkaTemplate<String, String> kafkaTemplate, ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicName;
        this.container = createContainer();
        this.container.start();
    }

    private KafkaMessageListenerContainer<String, String> createContainer() {
        ContainerProperties containerProperties = new ContainerProperties(facialAnalysisKafkaTopicName.getHumeFace());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                long offset = record.offset();
                consume(record.value(), offset);
            }
        });
        containerProperties.setGroupId(facialAnalysisKafkaTopicName.getHumeFace());
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consume(String record, long offset) {
        try {
            List<String> topEmotions = processPrediction(record);

            // Calculate startTime
            long startTime = (offset + 1) * 600;

            // Create a JSON object with "emotions" and "startTime" as the keys
            Map<String, Object> outputMap = new HashMap<>();
            outputMap.put("emotions", topEmotions);
            outputMap.put("startTime", startTime);

            // Convert the map to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonEmotionsWithTime = objectMapper.writeValueAsString(outputMap);

            // Send the JSON string to Kafka
            kafkaTemplate.send(facialAnalysisKafkaTopicName.getHumeFaceRanked(), jsonEmotionsWithTime);

        } catch (Exception e) {
            System.err.println("Error processing message in RawConsumer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> processPrediction(String jsonMessage) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonMessage);
        JsonNode prediction = rootNode.path("face").path("predictions").get(0);
        JsonNode emotionsArray = prediction.path("emotions");
        if (!emotionsArray.isArray()) {
            throw new IOException("'emotions' is not an array.");
        }

        Map<String, Float> emotionSumMap = new HashMap<>();
        Map<String, Integer> emotionCountMap = new HashMap<>();

        for (JsonNode emotionElement : emotionsArray) {
            String emotionName = emotionElement.path("name").asText();
            float score = (float) emotionElement.path("score").asDouble();
            emotionSumMap.put(emotionName, emotionSumMap.getOrDefault(emotionName, 0f) + score);
            emotionCountMap.put(emotionName, emotionCountMap.getOrDefault(emotionName, 0) + 1);
        }

        Map<String, Float> averagedEmotions = new HashMap<>();
        for (Map.Entry<String, Float> entry : emotionSumMap.entrySet()) {
            String emotion = entry.getKey();
            float averageScore = entry.getValue() / emotionCountMap.get(emotion);
            averagedEmotions.put(emotion, averageScore);
        }

        return averagedEmotions.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(3)
                .map(entry -> entry.getKey() + ": " + String.format("%.2f", entry.getValue()))
                .collect(Collectors.toList());
    }
}
