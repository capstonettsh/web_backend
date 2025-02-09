package com.communication.communication_backend.service.overallFeedback;

import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class GptResponseConsumer {

    private static final int FACIAL_INTERVAL = 600;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private final OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> speechContainer;
    private final KafkaMessageListenerContainer<String, String> facialContainer;
    private final Queue<JsonNode> speechMessagesQueue = new LinkedList<>();
    private List<JsonNode> facialEmotionData; // Add a list to hold facial emotion data

    @Autowired
    public GptResponseConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName,
                               FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName,
                               OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName,
                               KafkaTemplate<String, String> kafkaTemplate,
                               ConsumerFactory<String, String> consumerFactory) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicName;
        this.overallFeedbackKafkaTopicName = overallFeedbackKafkaTopicName;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;

        this.speechContainer = createSpeechContainer();
        this.facialContainer = createFacialContainer();
        this.speechContainer.start();
        this.facialContainer.start();
    }

    private KafkaMessageListenerContainer<String, String> createSpeechContainer() {
        ContainerProperties containerProperties = new ContainerProperties(
                toneAnalysisKafkaTopicName.getHumeSpeechGptResponse()
        );
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consumeSpeech(record.value());
            }
        });
        containerProperties.setGroupId("gpt-speech-response-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    private KafkaMessageListenerContainer<String, String> createFacialContainer() {
        ContainerProperties containerProperties = new ContainerProperties(
                facialAnalysisKafkaTopicName.getHumeFaceGPTResponse()
        );
        containerProperties.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                consumeFace(record.value());
            }
        });
        containerProperties.setGroupId("gpt-facial-response-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consumeSpeech(String record) {
        try {
            JsonNode jsonNode = objectMapper.readTree(record);
            processSpeechMessage(jsonNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void consumeFace(String record) {
        try {
            JsonNode jsonNode = objectMapper.readTree(record);
            facialEmotionData.add(jsonNode); // Store the facial emotion data
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processSpeechMessage(JsonNode jsonNode) throws Exception {
        // Extract necessary fields
        if (!jsonNode.has("userBeginTime") || !jsonNode.has("userEndTime")) {
            return;
        }

        int newUserBeginTime = jsonNode.get("userBeginTime").asInt();

        if (speechMessagesQueue.peek() != null) {
            JsonNode oldNode = speechMessagesQueue.peek();
            int oldUserBeginTime = oldNode.get("userBeginTime").asInt();
//            int oldUserEndTime = oldNode.get("userEndTime").asInt();
            // Get the facial emotion data for the exchange
            List<JsonNode> matchingFacialData = getFacialEmotionDataForExchange(facialEmotionData, oldUserBeginTime, newUserBeginTime);
            JsonNode aggregatedFacialEmotionData = aggregateFacialEmotionData(matchingFacialData);

            // Send combined data to _combined topic
            sendCombinedData(jsonNode, aggregatedFacialEmotionData);
        }

        speechMessagesQueue.add(jsonNode);
    }

    // Method to get facial emotion data for a specific exchange
    public List<JsonNode> getFacialEmotionDataForExchange(List<JsonNode> facialEmotionData, int oldUserBeginTime, int newUserBeginTime) {
        List<JsonNode> matchingFacialData = new ArrayList<>();
        JsonNode speechNode = speechMessagesQueue.remove();

        for (JsonNode facialEmotion : facialEmotionData) {
            int startTime = facialEmotion.get("startTime").asInt();
            if (startTime + FACIAL_INTERVAL/2 >= oldUserBeginTime && startTime <= newUserBeginTime) {
                matchingFacialData.add(facialEmotion);
            }
        }

        return matchingFacialData;
    }

    // Method to aggregate facial emotion data
    public JsonNode aggregateFacialEmotionData(List<JsonNode> facialEmotionData) {
        Map<String, Float> emotionScores = new HashMap<>();
        Map<String, Integer> emotionCounts = new HashMap<>();

        for (JsonNode facialEmotion : facialEmotionData) {
            JsonNode emotions = facialEmotion.get("emotions");
            for (Iterator<Map.Entry<String, JsonNode>> it = emotions.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String emotion = entry.getKey();
                float score = entry.getValue().floatValue();

                emotionScores.put(emotion, emotionScores.getOrDefault(emotion, 0f) + score);
                emotionCounts.put(emotion, emotionCounts.getOrDefault(emotion, 0) + 1);
            }
        }

        for (Map.Entry<String, Float> entry : emotionScores.entrySet()) {
            String emotion = entry.getKey();
            emotionScores.put(emotion, entry.getValue() / emotionCounts.get(emotion));
        }

        List<Map.Entry<String, Float>> sortedEmotions = new ArrayList<>(emotionScores.entrySet());
        sortedEmotions.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        Map<String, Float> top3Emotions = new HashMap<>();
        for (int i = 0; i < 3 && i < sortedEmotions.size(); i++) {
            top3Emotions.put(sortedEmotions.get(i).getKey(), sortedEmotions.get(i).getValue());
        }

        ObjectNode aggregatedFacialEmotion = objectMapper.createObjectNode();
        aggregatedFacialEmotion.set("emotions", objectMapper.valueToTree(top3Emotions));

        return aggregatedFacialEmotion;
    }

    // Send the combined data to _combined topic
    public void sendCombinedData(JsonNode speechData, JsonNode facialEmotionData) {
        ObjectNode combinedData = objectMapper.createObjectNode();
        combinedData.set("userMessage", speechData.get("userMessage"));
        combinedData.set("assistantMessage", speechData.get("assistantMessage"));
        combinedData.set("userStartTime", speechData.get("userBeginTime"));
        combinedData.set("userEndTime", speechData.get("userEndTime"));
        combinedData.set("facialEmotion", facialEmotionData);

        kafkaTemplate.send(overallFeedbackKafkaTopicName.getCombined(), combinedData.toString());
    }
}
