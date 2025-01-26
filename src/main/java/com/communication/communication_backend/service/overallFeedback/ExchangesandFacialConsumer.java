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
import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.toneAnalysis.FinalOpenAiClient;
import com.communication.communication_backend.service.toneAnalysis.OpenAiClient;
import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;

import java.util.*;

public class ExchangesandFacialConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    private final GptResponseConsumer gptResponseConsumer;
    @Autowired
    private OpenAiClient openAiClient;

    @Autowired
    private FinalOpenAiClient finalOpenAiClient;

    private final Queue<JsonNode> jsonNodeQueue = new LinkedList<>();
    private List<JsonNode> facialEmotionData = new ArrayList<>(); // To store facial emotion data for the whole session

    public ExchangesandFacialConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                             FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ConsumerFactory<String, String> consumerFactory,
                             GptResponseConsumer gptResponseConsumer) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicName;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.container = createContainer();
        this.container.start();
        this.gptResponseConsumer = gptResponseConsumer;

        // Start listening to facial emotion data topic as well
        listenToFacialEmotionData();
    }

    // Creating container to consume tone analysis responses
    private KafkaMessageListenerContainer<String, String> createContainer() {
        ContainerProperties containerProperties = new ContainerProperties(toneAnalysisKafkaTopicName.getHumeSpeechExchange());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consume(record.value());
            }
        });
        containerProperties.setGroupId(toneAnalysisKafkaTopicName.getHumeSpeechExchange());
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    // Consuming message and processing it
    public void consume(String exchangeJson) {
        try {
            JsonNode exchangeNode = objectMapper.readTree(exchangeJson);
            int userBeginTime = exchangeNode.get("userBeginTime").asInt();
            int userEndTime = exchangeNode.get("userEndTime").asInt();

            // Get facial emotion data for this exchange from the gptResponseConsumer
            List<JsonNode> facialEmotionDataForExchange = gptResponseConsumer.getFacialEmotionDataForExchange(facialEmotionData, userBeginTime, userEndTime);

            // Aggregate the facial emotion data for the exchange
            JsonNode aggregatedFacialEmotionData = gptResponseConsumer.aggregateFacialEmotionData(facialEmotionDataForExchange);

            // Send the combined data to _combined topic
            gptResponseConsumer.sendCombinedData(exchangeNode, aggregatedFacialEmotionData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Listen to facial emotion data from the topic
    private void listenToFacialEmotionData() {
        ContainerProperties containerProperties = new ContainerProperties(facialAnalysisKafkaTopicName.getHumeFaceGPTResponse());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consumeFacialEmotionData(record.value());
            }
        });
        containerProperties.setGroupId("facial-emotion-data-group");
        KafkaMessageListenerContainer<String, String> facialEmotionContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        facialEmotionContainer.start();
    }

    // Store facial emotion data from the topic
    private void consumeFacialEmotionData(String facialEmotionDataJson) {
        try {
            JsonNode facialEmotionNode = objectMapper.readTree(facialEmotionDataJson);
            facialEmotionData.add(facialEmotionNode); // Add to the list of all facial emotion data
        } catch (Exception e) {
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
            if (node.has("userMessage")) {
                String content = node.get("userMessage").asText();
                messages.add(Map.of("role", "user", "content", content));
            }

            if (node.has("assistantMessage")) {
                String content = node.get("assistantMessage").asText();
                messages.add(Map.of("role", "assistant", "content", content));
            }

            // Handle other possible fields if necessary
            // For example, timestamps or additional metadata
        }

        try {
            ObjectMapper objectMapper1 = new ObjectMapper();
            System.out.println("try check message" + messages);
            JsonNode feedback = finalOpenAiClient.getOverallFeedback(messages);
            kafkaTemplate.send(toneAnalysisKafkaTopicName.getOverallFeedback(), objectMapper1.writeValueAsString(feedback));
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
