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
import java.util.stream.Collectors;

public class ShortenedConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Intermediate logs for debugging
    private final List<String> intermediateLogs = Collections.synchronizedList(new ArrayList<>());
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;
    // Blocks
    private String previousBlockRole = null;
    private String previousBlockContent = null;
    private Map<String, Double> previousBlockEmotions = null;
    private String currentBlockRole = null;
    private StringBuilder currentBlockContent = null;
    private Map<String, Double> currentBlockEmotions = null;

    public ShortenedConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ConsumerFactory<String, String> consumerFactory) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.container = createContainer();
        this.container.start();
    }

    private KafkaMessageListenerContainer<String, String> createContainer() {
        ContainerProperties containerProperties = new ContainerProperties(toneAnalysisKafkaTopicName.getHumeSpeechShortened());
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consume(record.value());
            }
        });
        containerProperties.setGroupId("shortened-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consume(String message) {
        intermediateLogs.add(message);
        System.out.println("consume shortened message");

        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            // Detect end of conversation
            String role = jsonNode.has("role") ? jsonNode.get("role").asText() : null;
            if ("assistant".equals(role)) {
                // Conversation ended, flush what's left
                flush();
                return; // Stop processing since it's an end message
            }

//            String role = jsonNode.has("role") ? jsonNode.get("role").asText() : null;
            String content = jsonNode.has("content") ? jsonNode.get("content").asText() : null;

            if (role == null || content == null) {
                return;
            }

            // Parse top emotions
            Map<String, Double> messageEmotions = new HashMap<>();
            if (jsonNode.has("top_emotions")) {
                JsonNode emNode = jsonNode.get("top_emotions");
                emNode.fieldNames().forEachRemaining(e -> {
                    double val = emNode.get(e).asDouble();
                    messageEmotions.put(e, val);
                });
            }

            // Start or continue current block
            if (currentBlockRole == null) {
                // Initialize current block
                currentBlockRole = role;
                currentBlockContent = new StringBuilder(content);
                currentBlockEmotions = new HashMap<>();
                mergeEmotions(currentBlockEmotions, messageEmotions);
            } else if (role.equals(currentBlockRole)) {
                // Same role, accumulate
                currentBlockContent.append(" ").append(content);
                mergeEmotions(currentBlockEmotions, messageEmotions);
            } else {
                // Role changed, current block ended
                finalizeCurrentBlockAndHandleRoleChange();

                // Start new current block with the new role
                currentBlockRole = role;
                currentBlockContent = new StringBuilder(content);
                currentBlockEmotions = new HashMap<>();
                mergeEmotions(currentBlockEmotions, messageEmotions);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Call this method at the end of the conversation or on "assistant_end"
    // You'll need to detect "assistant_end" messages in consume() and then call flush()
    public void flush() {
        // If we still have a current block
        if (currentBlockRole != null) {
            // If there's a previous block, form an exchange
            if (previousBlockRole != null) {
                produceExchange(previousBlockRole, previousBlockContent, previousBlockEmotions,
                        currentBlockRole, currentBlockContent.toString(), currentBlockEmotions);
                clearPreviousBlock();
            } else {
                // Just produce a single-block exchange from current block
                produceSingleBlockExchange(currentBlockRole, currentBlockContent.toString(), currentBlockEmotions);
            }
            clearCurrentBlock();
        } else if (previousBlockRole != null) {
            // If we have only a previous block and no current block, produce single-block exchange
            produceSingleBlockExchange(previousBlockRole, previousBlockContent, previousBlockEmotions);
            clearPreviousBlock();
        }
    }

    private void finalizeCurrentBlockAndHandleRoleChange() {
        // currentBlock just finished
        if (previousBlockRole == null) {
            // Store currentBlock as previousBlock, waiting for a next block to form an exchange
            previousBlockRole = currentBlockRole;
            previousBlockContent = currentBlockContent.toString();
            previousBlockEmotions = new HashMap<>(currentBlockEmotions);
        } else {
            // We have a previous block and now a current block ended
            // produce exchange from previousBlock and currentBlock
            produceExchange(previousBlockRole, previousBlockContent, previousBlockEmotions,
                    currentBlockRole, currentBlockContent.toString(), currentBlockEmotions);

            // Clear previousBlock after producing exchange
            clearPreviousBlock();
        }

        // Clear currentBlock after moving it
        clearCurrentBlock();
    }

    private void produceExchange(String firstRole, String firstContent, Map<String, Double> firstEmotions,
                                 String secondRole, String secondContent, Map<String, Double> secondEmotions) {

        String firstWithEmotions = appendEmotionsToContent(firstContent, firstEmotions);
        String secondWithEmotions = appendEmotionsToContent(secondContent, secondEmotions);

        LinkedHashMap<String, String> exchange = new LinkedHashMap<>();
        exchange.put(firstRole, firstWithEmotions);
        exchange.put(secondRole, secondWithEmotions);

        String exchangeAsString = serializeExchange(exchange);
        System.out.println("produce exchange here1");
        kafkaTemplate.send(toneAnalysisKafkaTopicName.getHumeSpeechExchange(), exchangeAsString);
    }

    private void produceSingleBlockExchange(String role, String content, Map<String, Double> emotions) {
        String withEmotions = appendEmotionsToContent(content, emotions);

        LinkedHashMap<String, String> exchange = new LinkedHashMap<>();
        exchange.put(role, withEmotions);

        String exchangeAsString = serializeExchange(exchange);
        System.out.println("produce exchange here2");
        kafkaTemplate.send(toneAnalysisKafkaTopicName.getHumeSpeechExchange(), exchangeAsString);
    }

    private void mergeEmotions(Map<String, Double> target, Map<String, Double> source) {
        for (Map.Entry<String, Double> e : source.entrySet()) {
            target.put(e.getKey(), target.getOrDefault(e.getKey(), 0.0) + e.getValue());
        }
    }

    private String appendEmotionsToContent(String content, Map<String, Double> emotions) {
        if (emotions.isEmpty()) {
            return content;
        }

        // pick top 3 emotions by value
        List<Map.Entry<String, Double>> sorted = emotions.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        StringBuilder emotionString = new StringBuilder(" (");
        for (Map.Entry<String, Double> e : sorted) {
            emotionString.append(e.getKey()).append(": ")
                    .append(String.format("%.2f", e.getValue())).append(", ");
        }
        emotionString.setLength(emotionString.length() - 2);
        emotionString.append(")");

        return content + emotionString;
    }

    private String serializeExchange(Map<String, String> exchange) {
        try {
            return objectMapper.writeValueAsString(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private void clearPreviousBlock() {
        previousBlockRole = null;
        previousBlockContent = null;
        previousBlockEmotions = null;
    }

    private void clearCurrentBlock() {
        currentBlockRole = null;
        currentBlockContent = null;
        currentBlockEmotions = null;
    }

    public List<String> getIntermediateLogs() {
        synchronized (intermediateLogs) {
            return new ArrayList<>(intermediateLogs);
        }
    }
}
