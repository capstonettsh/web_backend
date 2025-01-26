package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class RawConsumer {

    // Consumes raw messages from Hume AI (with full tone data)
    // Extracts role, content, determines top 3 emotions, produces a shortened record to "shortened".
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    private final List<String> messages = new CopyOnWriteArrayList<>();

    public RawConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                       KafkaTemplate<String, String> kafkaTemplate,
                       ConsumerFactory<String, String> consumerFactory) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.container = createContainer();
        this.container.start(); // Start the container to begin consuming messages
    }

    private KafkaMessageListenerContainer<String, String> createContainer() {
        ContainerProperties containerProperties = new ContainerProperties(toneAnalysisKafkaTopicName.getHumeSpeech());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consume(record.value());
            }
        });
        containerProperties.setGroupId(toneAnalysisKafkaTopicName.getHumeSpeech()); // Set an appropriate group ID
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consume(String record) {
        System.out.println("testing for raw consumer please");
        try {
            JsonNode jsonNode = objectMapper.readTree(record);
//            System.out.println(jsonNode);

            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";
            if (!type.equals("assistant_message") && !type.equals("user_message")) {
                // We only process messages that have a user or assistant role
                // Adjust conditions as per your real schema
                return;
            }

            JsonNode messageNode = jsonNode.get("message");
            if (messageNode == null) return;

            String role = messageNode.has("role") ? messageNode.get("role").asText() : null;
            String content = messageNode.has("content") ? messageNode.get("content").asText() : null;
            if (role == null || content == null) return;

            // Extract scores (tone data)
            JsonNode scoresNode = jsonNode.at("/models/prosody/scores");
            Map<String, Double> emotionMap = new HashMap<>();
            if (scoresNode != null && scoresNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = scoresNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    emotionMap.put(entry.getKey(), entry.getValue().asDouble());
                }
            }

            // Get top 3 emotions
            LinkedHashMap<String, String> top3 = emotionMap.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(3)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.format("%.2f", e.getValue()),
                            (a, b) -> a, LinkedHashMap::new
                    ));

            Map<String, Object> shortened = new HashMap<>();

            // put start time if type is user_message
            if (type.equals("user_message")) {
                shortened.put("userBeginTime", jsonNode.get("time").get("begin").asText());
                shortened.put("userEndTime", jsonNode.get("time").get("end").asText());
            }

            shortened.put("role", role);
            shortened.put("content", content);
            shortened.put("top_emotions", top3);

            String shortenedMessage = objectMapper.writeValueAsString(shortened);
            kafkaTemplate.send(toneAnalysisKafkaTopicName.getHumeSpeechShortened(), shortenedMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
