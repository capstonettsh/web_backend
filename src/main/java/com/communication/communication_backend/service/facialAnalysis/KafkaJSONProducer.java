//package com.communication.communication_backend.service.facialAnalysis;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//@Service
//public class KafkaJSONProducer {
//
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    @Value("${kafka.topics.default:facialemoreview}")
//    private String topicName;
//
//    @Value("${json.file.path:src/main/resources/test.json}")
//    private String jsonFilePath;
//
//    public KafkaJSONProducer(KafkaTemplate<String, String> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    @PostConstruct
//    public void sendMessage() {
//        try {
//            String jsonMessage = Files.readString(Path.of(jsonFilePath));
//            kafkaTemplate.send(topicName, jsonMessage);
//            System.out.println("Message sent to topic: " + topicName);
//        } catch (IOException e) {
//            System.err.println("Error reading JSON file: " + jsonFilePath);
//            e.printStackTrace();
//        }
//    }
//}
