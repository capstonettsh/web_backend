package com.communication.communication_backend.service.overallFeedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


public class OverallFeedbackOpenAiClientTest {
    private OverallFeedbackOpenAiClient openAiClient;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        openAiClient = new OverallFeedbackOpenAiClient();

        // Injecting mockHttpClient via Reflection (since it's instantiated in the constructor)
        try {
            var httpClientField = OverallFeedbackOpenAiClient.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            httpClientField.set(openAiClient, mockHttpClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Set API URL and Key via Reflection
        try {
            var apiUrlField = OverallFeedbackOpenAiClient.class.getDeclaredField("API_URL");
            apiUrlField.setAccessible(true);
            apiUrlField.set(openAiClient, "https://api.openai.com/v1/chat/completions");

            var apiKeyField = OverallFeedbackOpenAiClient.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(openAiClient, "fake-api-key");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetOverallFeedback_SuccessfulResponse() throws Exception {
        // Arrange
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", "Hello!"),
                Map.of("role", "assistant", "content", "Hi, how can I help?")
        );

        String mockJsonResponse = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"overallFeedback\\": \\"Great job!\\", \\"top3Mistakes\\": []}"
                  }
                }
              ]
            }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockJsonResponse);
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockResponse);

        // Act
        JsonNode result = openAiClient.getOverallFeedback(messages);

        // Assert
        assertNotNull(result);
        assertEquals("Great job!", result.get("overallFeedback").asText());
        assertTrue(result.get("top3Mistakes").isArray());
        assertEquals(0, result.get("top3Mistakes").size());

        // Verify HTTP Request
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()));

        HttpRequest capturedRequest = requestCaptor.getValue();
        assertEquals(URI.create("https://api.openai.com/v1/chat/completions"), capturedRequest.uri());
        assertTrue(capturedRequest.headers().map().containsKey("Authorization"));
        assertTrue(capturedRequest.headers().map().containsKey("Content-Type"));
    }

    @Test
    void testGetOverallFeedback_ErrorResponse() throws Exception {
        // Arrange
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", "Hello!")
        );

        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"error\": \"Bad request\"}");
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockResponse);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> openAiClient.getOverallFeedback(messages));
        assertTrue(exception.getMessage().contains("Error: 400 - {\"error\": \"Bad request\"}"));
    }
}
