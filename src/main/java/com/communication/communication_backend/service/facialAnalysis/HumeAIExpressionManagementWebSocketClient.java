package com.communication.communication_backend.service.facialAnalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.nio.ByteBuffer;

public class HumeAIExpressionManagementWebSocketClient extends WebSocketClient {

    private final WebSocketSession frontendSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;

    public HumeAIExpressionManagementWebSocketClient(URI serverUri, WebSocketSession frontendSession,
                                                     KafkaTemplate<String, String> kafkaTemplate, FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName) {
        super(serverUri);
        this.frontendSession = frontendSession;
        this.kafkaTemplate = kafkaTemplate;
        this.facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicName;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        // Connection to Hume AI opened
        System.out.println("Connected to Hume AI Facial Expression");

        // Create the initial payload without the "type": "publish" field
//        ObjectNode initialPayload = objectMapper.createObjectNode();
//
//        // Define the models to use
//        ObjectNode modelsNode = initialPayload.putObject("models");
//
//        try {
//            // Read the image file
//            System.out.println("Current working directory: " + new File(".").getAbsolutePath());
//            File file = new File("/Users/ganchinsong/SUTD/web_backend/src/main/java/com/communication/communication_backend/service/people.jpeg");
//            byte[] fileContent = Files.readAllBytes(file.toPath());
//
//            // Encode the file’s bytes to Base64
//            String encodedImage = Base64.getEncoder().encodeToString(fileContent);
//
//            // Add the encoded image to the data field
//            initialPayload.put("data", encodedImage);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Configure the facial_expression model with minimal or empty settings
//        ObjectNode faceModelConfig = modelsNode.putObject("face");

        // Send the initial payload
        try {
//            System.out.println(initialPayload.toString() + "payload");
//            send(initialPayload.toString());
            System.out.println("Sent initial payload to Hume AI");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendImage(String base64Image) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode modelsNode = payload.putObject("models");
            modelsNode.putObject("face");
            payload.put("data", base64Image);

            String jsonPayload = payload.toString();

            send(jsonPayload);

//            System.out.println("Sent image payload to Hume AI: " + jsonPayload);
        } catch (Exception e) {
            System.out.println("Error sending image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String message) {
        kafkaTemplate.send(facialAnalysisKafkaTopicName.getHumeFace(), message);
//        System.out.println(message + "check here haha");
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        // Handle binary messages from Hume AI

        // Convert ByteBuffer to byte array
//        byte[] data = new byte[bytes.remaining()];
//        bytes.get(data);
//
//        // Convert binary data to string (assuming UTF-8 encoding)
//        String message = new String(data, StandardCharsets.UTF_8);
//        kafkaTemplate.send(facialAnalysisKafkaTopicName.getHumeFace(), message);

        // Parse the message
//            JsonNode rootNode = objectMapper.readTree(message);
//            String type = rootNode.get("type").asText();


    }

    public void closeStream() {
        // Send any final messages if required by Hume AI before closing
        try {
            String endMessage = "{\"type\":\"end_stream\"}";
            send(endMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Hume AI Facial Expression WebSocket closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
