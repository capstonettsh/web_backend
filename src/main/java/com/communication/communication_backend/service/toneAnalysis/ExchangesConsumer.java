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

import java.util.*;

public class ExchangesConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    @Autowired
    private OpenAiClient openAiClient;

    @Autowired
    private FinalOpenAiClient finalOpenAiClient;

    private final Queue<JsonNode> jsonNodeQueue = new LinkedList<>();

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
        containerProperties.setGroupId(toneAnalysisKafkaTopicName.getHumeSpeechExchange());
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consume(String exchangeJson) {
        try {
            JsonNode exchangeNode1 = objectMapper.readTree(exchangeJson);
            jsonNodeQueue.add(exchangeNode1);

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
