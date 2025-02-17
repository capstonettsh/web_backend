package com.communication.communication_backend.service.facialAnalysis;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class HumeAIExpressionManagementWebSocketClientTest {

    @Mock
    private WebSocketSession mockWebSocketSession;

    @Mock
    private KafkaTemplate<String, String> mockKafkaTemplate;

    @Mock
    private FacialAnalysisKafkaTopicName mockFacialAnalysisKafkaTopicName;

    private HumeAIExpressionManagementWebSocketClient webSocketClient;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        URI serverUri = new URI("ws://localhost:8080");  // Use a placeholder URI

        // Initialize the WebSocketClient with mocks
        when(mockFacialAnalysisKafkaTopicName.getHumeFace()).thenReturn("test-hume-face-topic");

        webSocketClient = new HumeAIExpressionManagementWebSocketClient(serverUri, mockWebSocketSession, mockKafkaTemplate, mockFacialAnalysisKafkaTopicName) {
            @Override
            public void sendPing() {
                // Mock behavior if necessary
            }
        };
    }

    @Test
    public void testSendImage() {
        // Arrange
        String base64Image = "fakeBase64EncodedImage";

        // Act
        webSocketClient.sendImage(base64Image);

        // There is no Mockito stub for `send`, post verification here as a placeholder
        // You should use integration tests to verify actual WebSocket behavior
    }

    @Test
    public void testOnMessage() {
        // Arrange
        String message = "test message";

        // Act
        webSocketClient.onMessage(message);

        // Assert
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(eq("test-hume-face-topic"), captor.capture());

        // Verify the message being sent to Kafka
        assertEquals(message, captor.getValue());
    }

    @Test
    public void testOnOpen() {
        ServerHandshake handshake = mock(ServerHandshake.class);
        assertDoesNotThrow(() -> webSocketClient.onOpen(handshake));
    }

    @Test
    public void testOnClose() {
        assertDoesNotThrow(() -> webSocketClient.onClose(1000, "Normal", true));
    }

    @Test
    public void testOnError() {
        Exception exception = new Exception("Test Exception");
        assertDoesNotThrow(() -> webSocketClient.onError(exception));
    }
}