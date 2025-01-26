package com.communication.communication_backend.service.overallFeedback;

import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import java.util.*;

public class OverallFeedbackExchangesConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    @Autowired
    private OverallFeedbackOpenAiClient openAiClient;

    private Queue<JsonNode> jsonNodeQueue = new LinkedList<JsonNode>();

    public OverallFeedbackExchangesConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName, OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName,
                                            KafkaTemplate<String, String> kafkaTemplate,
                                            ConsumerFactory<String, String> consumerFactory) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.overallFeedbackKafkaTopicName = overallFeedbackKafkaTopicName;
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
        containerProperties.setGroupId("overall-feedback-exchange-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consume(String exchangeJson) {
        try {
            System.out.println("Do you exchange");
            JsonNode exchangeNode = objectMapper.readTree(exchangeJson);
            jsonNodeQueue.add(exchangeNode);
        } catch (Exception e) {
            System.err.println("Error processing exchange with ChatGPT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public JsonNode endChat() {
        if (jsonNodeQueue.isEmpty()) {
            System.err.println("No messages to process for feedback.");
            return null;
        }

        // Prepare the messages list by dumping the entire queue
        List<Map<String, Object>> messages = new ArrayList<>();

        for (JsonNode node : jsonNodeQueue) {
            String role = node.has("role") ? node.get("role").asText() : "user";
            JsonNode contentNode = node.get("content");

            // Assuming content is an array of objects with "type" and "text"
            if (contentNode != null && contentNode.isArray()) {
                StringBuilder contentBuilder = new StringBuilder();
                for (JsonNode contentObj : contentNode) {
                    if (contentObj.has("type") && "text".equals(contentObj.get("type").asText())
                            && contentObj.has("text")) {
                        contentBuilder.append(contentObj.get("text").asText()).append(" ");
                    }
                }
                String content = contentBuilder.toString().trim();
                messages.add(Map.of("role", role, "content", content));
            } else if (contentNode != null && contentNode.isTextual()) {
                messages.add(Map.of("role", role, "content", contentNode.asText()));
            } else {
                System.err.println("Unsupported content structure in exchange: " + node.toString());
            }
        }

        try {
            ObjectMapper objectMapper1 = new ObjectMapper();
            System.out.println("try check message" + messages);
            JsonNode feedback = openAiClient.getOverallFeedback(messages);
            kafkaTemplate.send(overallFeedbackKafkaTopicName.getOverallFeedback(), objectMapper1.writeValueAsString(feedback));
            // Optionally, clear the queue after processing
            jsonNodeQueue.clear();
            return feedback;
        } catch (Exception e) {
            System.err.println("Error getting overall feedback: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
