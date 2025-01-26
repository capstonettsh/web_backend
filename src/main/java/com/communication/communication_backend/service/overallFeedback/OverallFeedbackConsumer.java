package com.communication.communication_backend.service.overallFeedback;

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

import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.overallFeedback.OverallFeedbackKafkaTopicName;

import java.util.*;

public class OverallFeedbackConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private final OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    
    @Autowired
    private OverallFeedbackOpenAiClient overallFeedbackOpenAiClient;

    @Autowired
    private GptResponseConsumer gptResponseConsumer;

    private final Queue<JsonNode> jsonNodeQueue = new LinkedList<>();

    public OverallFeedbackConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                                   FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName,
                                   OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ConsumerFactory<String, String> consumerFactory) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicName;
        this.overallFeedbackKafkaTopicName = overallFeedbackKafkaTopicName;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.container = createContainer();
        this.container.start();
    }

    // Creating container to consume data from _combined topic
    private KafkaMessageListenerContainer<String, String> createContainer() {
        ContainerProperties containerProperties = new ContainerProperties(facialAnalysisKafkaTopicName.getHumeFaceGPTResponse());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consumeCombinedData(record.value());
            }
        });
        containerProperties.setGroupId("combined-feedback-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    // Consuming the combined data
    public void consumeCombinedData(String combinedJson) {
        try {
            JsonNode combinedNode = objectMapper.readTree(combinedJson);
            Map<String, Object> combinedDataMap = new HashMap<>();
            combinedDataMap.put("userMessage", combinedNode.get("userMessage").asText());
            combinedDataMap.put("assistantMessage", combinedNode.get("assistantMessage").asText());
            combinedDataMap.put("userStartTime", combinedNode.get("userStartTime").asDouble());
            combinedDataMap.put("userEndTime", combinedNode.get("userEndTime").asDouble());
            combinedDataMap.put("facialEmotion", combinedNode.get("facialEmotion"));

            // Adding this to the queue to later pass into getOverallFeedback
            jsonNodeQueue.add(objectMapper.valueToTree(combinedDataMap));

            if (jsonNodeQueue.size() >= 3) {
                // Call getOverallFeedback once we have enough messages
                List<Map<String, Object>> messages = new ArrayList<>();
                for (JsonNode node : jsonNodeQueue) {
                    Map<String, Object> messageMap = objectMapper.convertValue(node, Map.class);
                    messages.add(messageMap);
                }

                JsonNode overallFeedback = overallFeedbackOpenAiClient.getOverallFeedback(messages);

                // Send overall feedback to _overall_feedback topic
                kafkaTemplate.send(overallFeedbackKafkaTopicName.getOverallFeedback(), overallFeedback.toString());

                // Clear the queue after processing
                jsonNodeQueue.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
