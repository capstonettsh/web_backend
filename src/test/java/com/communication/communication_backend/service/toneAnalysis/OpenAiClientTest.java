package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OpenAiClientTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private OpenAiClient openAiClient;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Instantiate OpenAiClient with mock HttpClient
        openAiClient = new OpenAiClient();
        setPrivateField(openAiClient, "httpClient", mockHttpClient);
        setPrivateField(openAiClient, "API_URL", "http://fake-url.com/api");
        setPrivateField(openAiClient, "apiKey", "test-api-key");
    }

    @Test
    public void testGetEmpathyRating_successfulResponse() throws Exception {
        // Arrange
        String conversation = "Sample conversation";
        String responseJson = "{ \"choices\": [{ \"message\": { \"content\": \"{ \\\"value\\\": 7 }\" } }] }";

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(responseJson);

        // Act
        String result = openAiClient.getEmpathyRating(conversation);

        // Assert
        assertEquals("7", result);
    }

    @Test
    public void testGetEmpathyRating_errorResponse() throws Exception {
        // Arrange
        String conversation = "Sample conversation";

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("{ \"error\": \"Internal server error\" }");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            openAiClient.getEmpathyRating(conversation);
        });
    }

    // Reflection utility to set private fields
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}