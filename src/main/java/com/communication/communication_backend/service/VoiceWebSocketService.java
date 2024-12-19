package com.communication.communication_backend.service;

import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class VoiceWebSocketService extends BinaryWebSocketHandler {

    private ByteArrayOutputStream audioData;
    private HumeAIWebSocketClient humeAIClient;

    @Autowired
    private ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;

    @Autowired
    private ApplicationContext context;

    @Value("${humeai.api.key}")
    private String humeAiApiKey;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Initialize ByteArrayOutputStream to collect audio data
        audioData = new ByteArrayOutputStream();

        String userId = "1";
        String sessionDateTime = generateSessionDateTime();
        toneAnalysisKafkaTopicName.setSessionDateTime(sessionDateTime);
        toneAnalysisKafkaTopicName.setUserId(userId);

        URI humeUri = new URI("wss://api.hume.ai/v0/evi/chat?api_key=" + humeAiApiKey + "&config_id=cc18cb53-3f0e" +
                "-4d99-9a93" +
                "-b318b1352496");

        humeAIClient = context.getBean(HumeAIWebSocketClient.class, humeUri, session,
                toneAnalysisKafkaTopicName);
        humeAIClient.connectBlocking();
    }

    private String generateSessionDateTime() {
        // Format the current datetime as yyyy-MM-ddTHH-mm-ss to avoid issues with colons
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        audioData.write(message.getPayload().array());

        if (humeAIClient != null && humeAIClient.isOpen()) {
            humeAIClient.sendAudioData(message.getPayload().array());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (humeAIClient != null && humeAIClient.isOpen()) {
            humeAIClient.closeStream();
        }

        audioData.close();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        exception.printStackTrace();
    }
}
