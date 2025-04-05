package com.communication.communication_backend.service;

import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class PerConnectionWebSocketHandler implements WebSocketHandler {
  private final ApplicationContext context;

  public PerConnectionWebSocketHandler(ApplicationContext context) {
    this.context = context;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    StreamingWebSocketHandler handler = context.getBean(StreamingWebSocketHandler.class);
    session.getAttributes().put("handler", handler);
    handler.afterConnectionEstablished(session);
  }

  @Override
  public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
    StreamingWebSocketHandler handler = (StreamingWebSocketHandler) session.getAttributes().get("handler");
    if (handler != null) {
      handler.handleMessage(session, message);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

  }

  @Override
  public boolean supportsPartialMessages() {
    return false;
  }
}
