package com.communication.communication_backend.controller;

import com.communication.communication_backend.service.StreamingWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatControllerTest {

    @Mock
    private StreamingWebSocketHandler mockStreamingWebSocketHandler;

    @Mock
    private WebSocketHandlerRegistry mockRegistry;

    private ChatController chatController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        chatController = new ChatController();
        setPrivateField(chatController, "streamingWebSocketHandler", mockStreamingWebSocketHandler);
    }

    @Test
    public void testCreateWebSocketContainer() {
        // Act
        ServletServerContainerFactoryBean container = chatController.createWebSocketContainer();

        // Assert
        assertEquals(500 * 1024, container.getMaxTextMessageBufferSize());
        assertEquals(500 * 1024, container.getMaxBinaryMessageBufferSize());
    }

    // Reflection utility to set private fields
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Error accessing field: " + fieldName, e);
        }
    }
}