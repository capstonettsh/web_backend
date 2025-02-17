package com.communication.communication_backend.service;

import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.facialAnalysis.HumeAIExpressionManagementWebSocketClient;
import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class WebSocketClientConfigTest {

    @Mock
    private KafkaTemplate<String, String> mockKafkaTemplate;

    private WebSocketClientConfig webSocketClientConfig;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        webSocketClientConfig = new WebSocketClientConfig(mockKafkaTemplate);
    }

    @Test
    public void testHumeAIWebSocketClientCreation() throws Exception {
        // Arrange
        URI serverUri = new URI("ws://localhost:8080/client");
        WebSocketSession mockSession = mock(WebSocketSession.class);
        ToneAnalysisKafkaTopicName mockTopicName = mock(ToneAnalysisKafkaTopicName.class);

        // Act
        HumeAIAudioWebSocketClient client = webSocketClientConfig.humeAIWebSocketClient(serverUri, mockSession, mockTopicName);

        // Assert
        assertNotNull(client);
        // Additional verification to check correct setup, if necessary
        // Example: Verify that client uses expected KafkaTemplate, session, etc.
    }

    @Test
    public void testHumeAIExpressionManagementWebSocketClientCreation() throws Exception {
        // Arrange
        URI serverUri = new URI("ws://localhost:8080/expression");
        WebSocketSession mockSession = mock(WebSocketSession.class);
        FacialAnalysisKafkaTopicName mockFacialTopicName = mock(FacialAnalysisKafkaTopicName.class);

        // Act
        HumeAIExpressionManagementWebSocketClient expressionClient = webSocketClientConfig.humeAIExpressionManagementWebSocketClient(serverUri, mockSession, mockFacialTopicName);

        // Assert
        assertNotNull(expressionClient);
        // Additional verification to check the correct setup can be done here
    }
}