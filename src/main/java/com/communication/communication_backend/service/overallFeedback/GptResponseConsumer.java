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

import java.util.LinkedList;
import java.util.Queue;

public class GptResponseConsumer {
    private static final long BUFFER_TIME_THRESHOLD = 60000; // e.g., 60 seconds
    private static final int FACIAL_INTERVAL = 300; // 300 ms
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private final OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> speechContainer;
    private final KafkaMessageListenerContainer<String, String> facialContainer;
    private final Queue<JsonNode> speechMessagesQueue = new LinkedList<>();
    private final Queue<JsonNode> faceMessagesQueue = new LinkedList<>();

    public GptResponseConsumer(ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName, FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName, OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName, KafkaTemplate<String, String> kafkaTemplate, ConsumerFactory<String, String> consumerFactory) {
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
        this.facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicName;
        this.overallFeedbackKafkaTopicName = overallFeedbackKafkaTopicName;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;

        this.speechContainer = createSpeechContainer();
        this.speechContainer.start();

        this.facialContainer = createFacialContainer();
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
//        containerProperties.setMessageListener((MessageListener<String, String>) this::consumeFace);
        containerProperties.setGroupId("gpt-facial-response-group");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public void consumeSpeech(String record) {
        try {
            JsonNode jsonNode = objectMapper.readTree(record);

            Thread.sleep(5000); // wait for facial analysis data
            processSpeechMessage(jsonNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void consumeFace(String record) {
        try {
            JsonNode jsonNode = objectMapper.readTree(record);

            processFaceMessage(jsonNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processSpeechMessage(JsonNode jsonNode) throws JsonProcessingException {
        // Validate and extract necessary fields
        if (!jsonNode.has("userBeginTime") || !jsonNode.has("userEndTime")) {
            return;
        }

        int newUserBeginTime = jsonNode.get("userBeginTime").asInt();
//        int newUserEndTime = jsonNode.get("userEndTime").asInt();

        if (speechMessagesQueue.peek() != null) {
            JsonNode oldNode = speechMessagesQueue.peek();
            int oldUserBeginTime = oldNode.get("userBeginTime").asInt();
//            int oldUserEndTime = oldNode.get("userEndTime").asInt();

            addFacialDataAndPush(oldUserBeginTime, newUserBeginTime);
        }

        speechMessagesQueue.add(jsonNode);
    }

    private void processFaceMessage(JsonNode jsonNode) {
        faceMessagesQueue.add(jsonNode);
    }

    private void addFacialDataAndPush(int oldUserBeginTime, int newUserBeginTime) throws JsonProcessingException {
        // add new speech message and all the relevant facial analysis data into the json node
        ObjectMapper objectMapperCombined = new ObjectMapper();
        JsonNode speechNode = speechMessagesQueue.remove();

        ObjectNode speechObjectNode = (ObjectNode) speechNode;

        // Create an ArrayNode to hold the `anotherJsonNode`
        ArrayNode facialAnalysisArray = JsonNodeFactory.instance.arrayNode();

        while (true) {
            if (faceMessagesQueue.peek() == null) {
                // todo push the data
//                ObjectNode facialAnalysisObject = JsonNodeFactory.instance.objectNode();
//                facialAnalysisObject.set("facialAnalysis", facialAnalysisArray);
                speechObjectNode.set("facialAnalysis", facialAnalysisArray);
                kafkaTemplate.send(overallFeedbackKafkaTopicName.getCombined(), objectMapperCombined.writeValueAsString(speechObjectNode));
                break;
            }

            // if it's within range
            if (faceMessagesQueue.peek().get("startTime").asInt() + FACIAL_INTERVAL / 2 >= oldUserBeginTime &&
                    faceMessagesQueue.peek().get("startTime").asInt() <= newUserBeginTime
            ) {
                // append this facial message
                facialAnalysisArray.add(faceMessagesQueue.remove());
            } else if (faceMessagesQueue.peek().get("startTime").asInt() + FACIAL_INTERVAL / 2 <= oldUserBeginTime) {
                faceMessagesQueue.remove(); // remove unused node
            } else {
                // face analysis is out of range, proceed to push the data to kafka
                // todo push the data
//                ObjectNode facialAnalysisObject = JsonNodeFactory.instance.objectNode();
//                facialAnalysisObject.set("facialAnalysis", facialAnalysisArray);

                // Append the new structure to speechObjectNode
                speechObjectNode.set("facialAnalysis", facialAnalysisArray);
                kafkaTemplate.send(overallFeedbackKafkaTopicName.getCombined(), objectMapperCombined.writeValueAsString(speechObjectNode));
                break;
            }
        }
    }

    public void endChat() {
        try {
            // Check if there is a speech message to process
            if (speechMessagesQueue.isEmpty()) {
                System.out.println("No speech messages to process.");
                return;
            }
            Thread.sleep(5000); // wait for facial analysis data

            // Remove the last speech message from the queue
            JsonNode speechNode = speechMessagesQueue.remove();
            ObjectNode speechObjectNode = (ObjectNode) speechNode;

            // Create an ArrayNode to hold the facial analysis data
            ArrayNode facialAnalysisArray = JsonNodeFactory.instance.arrayNode();

            // Add all remaining facial messages to the facialAnalysisArray
            while (!faceMessagesQueue.isEmpty()) {
                facialAnalysisArray.add(faceMessagesQueue.remove());
            }

            // Create an object to hold the facial analysis data
            ObjectNode facialAnalysisObject = JsonNodeFactory.instance.objectNode();
            facialAnalysisObject.set("facialAnalysis", facialAnalysisArray);

            // Append the facial analysis data to the speech message
            speechObjectNode.set("facialAnalysis", facialAnalysisObject);

            // Send the combined data to the Kafka topic
            kafkaTemplate.send(overallFeedbackKafkaTopicName.getCombined(), objectMapper.writeValueAsString(speechObjectNode));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
