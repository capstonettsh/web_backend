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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class FinalOpenAiClientTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private FinalOpenAiClient finalOpenAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Construct FinalOpenAiClient and inject mock dependencies using reflection
        finalOpenAiClient = new FinalOpenAiClient();
        setPrivateField(finalOpenAiClient, "httpClient", mockHttpClient);
        setPrivateField(finalOpenAiClient, "API_URL", "http://fake-url.com/api");
        setPrivateField(finalOpenAiClient, "apiKey", "test-api-key");
    }

    @Test
    public void testGetOverallFeedback_successfulResponse() throws Exception {
        // Arrange
        String responseJson = "{ \"choices\": [{ \"message\": { \"content\": \"{ \\\"overallFeedback\\\": \\\"Great!\\\", \\\"top3Mistakes\\\": [] }\" } }] }";
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(responseJson);

        List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", "Sample message"));

        // Act
        JsonNode result = finalOpenAiClient.getOverallFeedback(messages);

        // Assert
        assertEquals("Great!", result.get("overallFeedback").asText());
        assertEquals(0, result.get("top3Mistakes").size());
    }

    @Test
    public void testGetOverallFeedback_errorResponse() throws Exception {
        // Arrange
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("{ \"error\": \"Internal server error\" }");

        List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", "Sample message"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            finalOpenAiClient.getOverallFeedback(messages);
        });

        assertEquals("Error: 500 - { \"error\": \"Internal server error\" }", exception.getMessage());
    }

    // Reflection utility to set private fields
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
