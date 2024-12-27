//package com.communication.communication_backend.service.facialAnalysis;
//
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//public class ChatGPTProducer {
//
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    public ChatGPTProducer(KafkaTemplate<String, String> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    public void sendRatedResponse(String response, String topic) {
//        kafkaTemplate.send(topic, response);
//        System.out.println("Sent ChatGPT response to topic: " + topic);
//    }
//}
