package com.communication.communication_backend.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

@Configuration
public class WebSocketClientConfig {
    @Bean
    @Scope("prototype")
    public HumeAIWebSocketClient humeAIWebSocketClient(URI serverUri, WebSocketSession frontendSession) {
        return new HumeAIWebSocketClient(serverUri, frontendSession);
    }
}
