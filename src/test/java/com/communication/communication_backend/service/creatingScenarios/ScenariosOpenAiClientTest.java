package com.communication.communication_backend.service.creatingScenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenariosOpenAiClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScenariosOpenAiClient scenariosOpenAiClient;

    @BeforeEach
    void setUp() throws Exception {
        // Inject mock HttpClient and ObjectMapper via reflection
        setPrivateField(scenariosOpenAiClient, "httpClient", httpClient);
        setPrivateField(scenariosOpenAiClient, "objectMapper", objectMapper);

        // Set API_URL and apiKey via reflection
        setPrivateField(scenariosOpenAiClient, "API_URL", "https://api.openai.com/v1/chat/completions");
        setPrivateField(scenariosOpenAiClient, "apiKey", "test-api-key");
    }

    @Test
    void testGetGeneratedResponse_Success() throws Exception {
        // Arrange
        List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", "test message"));
        String expectedRequestBodyJson = "{\"model\":\"gpt-4o\",\"messages\":[],\"response_format\":{}}"; // Simplified for test
        String expectedResponse = "{\"choices\":[{\"message\":{\"content\":\"{\\\"generatedScenario\\\":[]}\"}}]}";

        // Mock ObjectMapper for request serialization
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedRequestBodyJson);

        // Mock HttpClient behavior
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(expectedResponse);

        // Mock ObjectMapper for response parsing
        JsonNode mockJsonResponse = new ObjectMapper().readTree(expectedResponse);
        JsonNode mockContentJson = new ObjectMapper().readTree("{\"generatedScenario\":[]}");
        when(objectMapper.readTree(expectedResponse)).thenReturn(mockJsonResponse);
        when(objectMapper.readTree("{\"generatedScenario\":[]}")).thenReturn(mockContentJson);

        // Act
        JsonNode response = scenariosOpenAiClient.getGeneratedResponse(messages);

        // Assert
        assertNotNull(response);
        assertTrue(response.has("generatedScenario"));
        assertTrue(response.get("generatedScenario").isArray());

        // Verify interactions
        verify(objectMapper, times(1)).writeValueAsString(any());
        verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        verify(objectMapper, times(1)).readTree(expectedResponse);
        verify(objectMapper, times(1)).readTree("{\"generatedScenario\":[]}");
    }

    @Test
    void testGetGeneratedResponse_ApiError() throws Exception {
        // Arrange
        List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", "test message"));
        String expectedRequestBodyJson = "{\"model\":\"gpt-4o\",\"messages\":[],\"response_format\":{}}"; // Simplified for test
        String errorResponse = "Internal Server Error";

        // Mock ObjectMapper for request serialization
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedRequestBodyJson);

        // Mock HttpClient behavior
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn(errorResponse);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            scenariosOpenAiClient.getGeneratedResponse(messages);
        });

        assertTrue(exception.getMessage().contains("Error: 500"), "Exception message should contain 'Error: 500'");
        assertTrue(exception.getMessage().contains(errorResponse), "Exception message should contain response body");

        // Verify interactions
        verify(objectMapper, times(1)).writeValueAsString(any());
        verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    // Utility to set private fields via reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}