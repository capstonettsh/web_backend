package com.communication.communication_backend.service.toneAnalysis;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatGPTProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final List<String> empathyRatedExchanges = new ArrayList<>();

    public ChatGPTProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRatedExchange(String exchange, String topic) {
        kafkaTemplate.send(topic, exchange);
        synchronized (empathyRatedExchanges) {
            empathyRatedExchanges.add(exchange);
        }
        System.out.println("Sent empathy-rated exchange to topic: " + topic + " - " + exchange);
    }

    public List<String> getEmpathyRatedExchanges() {
        synchronized (empathyRatedExchanges) {
            return new ArrayList<>(empathyRatedExchanges);
        }
    }
}
