package com.communication.communication_backend.service.facialAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FacialRankedConsumer {

    //    private final KafkaTemplate<String, String> kafkaTemplate;
    private final List<String> gptResponses = new ArrayList<>();
    private final List<String> chunksProcessed = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    //    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    @Autowired
    private FacialOpenAiClient facialOpenAiClient;

    public FacialRankedConsumer(FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName, KafkaTemplate<String, String> kafkaTemplate, ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicName;
        this.container = createContainer();
        this.container.start();
    }

    private KafkaMessageListenerContainer<String, String> createContainer() {
        ContainerProperties containerProperties = new ContainerProperties(facialAnalysisKafkaTopicName.getHumeFaceRanked());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consume(record.value());
            }
        });
        containerProperties.setGroupId("facial-raw-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consume(String chunk) {
        try {
            System.out.println("Processing chunk with ChatGPT: " + chunk);

            // Send chunk to ChatGPT and get response
            String gptResponse = facialOpenAiClient.getEmpathyRating(chunk);

            // Extract "content" field from GPT response
            String content = extractContentFromResponse(gptResponse);

            // Append chunk-response pair to gptResponses
            String chunkResponsePair = chunk.trim() + System.lineSeparator() + System.lineSeparator() + content.trim();
            synchronized (gptResponses) {
                gptResponses.add(chunkResponsePair);
            }

            // Send response to gpt-rated-emotions topic
            kafkaTemplate.send(facialAnalysisKafkaTopicName.getHumeFaceGPTResponse(), gptResponse);
//            System.out.println("Sent GPT response to topic: " + gptTopic);
        } catch (Exception e) {
            System.err.println("Error processing chunk with ChatGPT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractContentFromResponse(String gptResponse) throws IOException {
        JsonNode rootNode = objectMapper.readTree(gptResponse);
        JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
        return contentNode.asText();
    }

    private boolean allChunksProcessed() {
        // Check if all chunks have been processed (logic depends on your tracking implementation)
        synchronized (chunksProcessed) {
            return !chunksProcessed.isEmpty() && gptResponses.size() == chunksProcessed.size();
        }
    }
}
