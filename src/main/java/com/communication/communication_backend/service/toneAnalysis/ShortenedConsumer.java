package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import java.util.HashMap;
import java.util.Map;

public class ShortenedConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaMessageListenerContainer<String, String> container;

    private boolean isUserAppeared = false;
    private boolean isAssistantAppeared = false;

    private ToneShortened[] toneShortenedBuffer = new ToneShortened[0];

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
//        intermediateLogs.add(message);
        System.out.println("consume shortened message");

        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            // Detect end of conversation
            String role = jsonNode.has("role") ? jsonNode.get("role").asText() : null;
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


            processShortenedMessage(new ToneShortened(role, content, messageEmotions));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processShortenedMessage(ToneShortened toneShortened) {
        addToToneShortenedBuffer(toneShortened);

        String role = toneShortened.role();

        if (role.equals("user") && isUserAppeared && isAssistantAppeared) { // if user appeared again, then flush the message
            flush();
        } else {
            // update state
            if (role.equals("user")) {
                isUserAppeared = true;
            } else if (role.equals("assistant")) {
                isAssistantAppeared = true;
            }
        }
    }

    private void addToToneShortenedBuffer(ToneShortened newElement) {
        if (toneShortenedBuffer == null) {
            toneShortenedBuffer = new ToneShortened[]{newElement};
        } else {
            ToneShortened[] newBuffer = new ToneShortened[toneShortenedBuffer.length + 1];
            System.arraycopy(toneShortenedBuffer, 0, newBuffer, 0, toneShortenedBuffer.length);
            newBuffer[toneShortenedBuffer.length] = newElement;
            toneShortenedBuffer = newBuffer;
        }
    }

    // Call this method at the end of the conversation or on "assistant_end"
    // You'll need to detect "assistant_end" messages in consume() and then call flush()
    public void flush() {
        ExchangeMessage exchangeMessage = produceExchangeMessage();

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String jsonMessage = objectMapper.writeValueAsString(exchangeMessage);
            kafkaTemplate.send(toneAnalysisKafkaTopicName.getHumeSpeechExchange(), jsonMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        toneShortenedBuffer = new ToneShortened[0];
        isUserAppeared = false;
        isAssistantAppeared = false;
    }

    private ExchangeMessage produceExchangeMessage() {
        StringBuilder userMessageBuilder = new StringBuilder();
        StringBuilder assistantMessageBuilder = new StringBuilder();

        // Aggregate messages from the buffer based on role
        for (ToneShortened ts : toneShortenedBuffer) {
            String contentWithEmotions = appendEmotionsToContent(ts.content(), ts.messageEmotions());
            if ("user".equalsIgnoreCase(ts.role())) {
                userMessageBuilder.append(contentWithEmotions).append(" ");
            } else if ("assistant".equalsIgnoreCase(ts.role())) {
                assistantMessageBuilder.append(contentWithEmotions).append(" ");
            }
        }

        String userMessage = userMessageBuilder.toString().trim();
        String assistantMessage = assistantMessageBuilder.toString().trim();

        ExchangeMessage exchangeMessage = new ExchangeMessage(userMessage, assistantMessage);
        return exchangeMessage;
    }

    private String appendEmotionsToContent(String content, Map<String, Double> messageEmotions) {
        if (messageEmotions == null || messageEmotions.isEmpty()) {
            return content;
        }

        // Format emotions as "emotion1=score1, emotion2=score2, ..."
        StringBuilder emotionsBuilder = new StringBuilder("[Emotions: ");
        messageEmotions.forEach((emotion, score) -> {
            emotionsBuilder.append(emotion).append("=").append(String.format("%.2f", score)).append(", ");
        });

        // Remove the trailing comma and space
        if (emotionsBuilder.length() > 11) { // Length of "[Emotions: "
            emotionsBuilder.setLength(emotionsBuilder.length() - 2);
        }
        emotionsBuilder.append("]");

        // Split content into sentences (simple split based on period)
        String[] sentences = content.split("(?<=[.!?])\\s+");
        StringBuilder contentWithEmotions = new StringBuilder();

        for (String sentence : sentences) {
            contentWithEmotions.append(sentence.trim()).append(" ").append(emotionsBuilder).append(" ");
        }

        return contentWithEmotions.toString().trim();
    }

    public void endChat() {
        flush();
    }

    private record ExchangeMessage(String userMessage, String assistantMessage) {
    }
}
