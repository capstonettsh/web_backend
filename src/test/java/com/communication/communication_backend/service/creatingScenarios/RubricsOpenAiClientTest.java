package com.communication.communication_backend.service.creatingScenarios;

import com.communication.communication_backend.service.creatingScenarios.RubricsOpenAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//this is the correct
public class RubricsOpenAiClientTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Mock
    private ObjectMapper mockObjectMapper;

    @InjectMocks
    private RubricsOpenAiClient rubricsOpenAiClient;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Manually inject mocks using reflection since constructor initializes fields
        rubricsOpenAiClient = new RubricsOpenAiClient();
        setPrivateField(rubricsOpenAiClient, "httpClient", mockHttpClient);
        setPrivateField(rubricsOpenAiClient, "objectMapper", mockObjectMapper);
        setPrivateField(rubricsOpenAiClient, "API_URL", "http://fake-url.com/api");
        setPrivateField(rubricsOpenAiClient, "apiKey", "test-api-key");
    }

    @Test
    public void testGetGeneratedRubrics_success() throws Exception {
        // Arrange
        String responseJson = "{ \"choices\": [{ \"message\": { \"content\": \"{\\\"generatedRubrics\\\":[{\\\"title\\\":\\\"Communication Skills\\\",\\\"unsatisfactory\\\":\\\"Poor\\\",\\\"borderline\\\":\\\"Adequate\\\",\\\"satisfactory\\\":\\\"Excellent\\\"}]}\" } }] }";
        String contentJson = "{\"generatedRubrics\":[{\"title\":\"Communication Skills\",\"unsatisfactory\":\"Poor\",\"borderline\":\"Adequate\",\"satisfactory\":\"Excellent\"}]}";

        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(responseJson);

        JsonNode responseNode = new ObjectMapper().readTree(responseJson);
        JsonNode contentNode = new ObjectMapper().readTree(contentJson);
        when(mockObjectMapper.readTree(responseJson)).thenReturn(responseNode);
        when(mockObjectMapper.readTree(contentJson)).thenReturn(contentNode);
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("mockedRequestBodyJson");

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", "Generate rubrics for a mock patient scenario titled 'Communication Skills'")
        );

        // Act
        JsonNode result = rubricsOpenAiClient.getGeneratedRubrics(messages);

        // Assert
        assertNotNull(result);
        assertTrue(result.has("generatedRubrics"));
        JsonNode rubrics = result.get("generatedRubrics");
        assertEquals(1, rubrics.size());
        JsonNode rubric = rubrics.get(0);
        assertEquals("Communication Skills", rubric.get("title").asText());
        assertEquals("Poor", rubric.get("unsatisfactory").asText());
        assertEquals("Adequate", rubric.get("borderline").asText());
        assertEquals("Excellent", rubric.get("satisfactory").asText());
    }

    @Test
    public void testGetGeneratedRubrics_errorResponse() throws Exception {
        // Arrange
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("{ \"error\": \"Internal server error\" }");
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("mockedRequestBodyJson");

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", "Generate rubrics for a mock patient scenario")
        );

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            rubricsOpenAiClient.getGeneratedRubrics(messages);
        });

        assertEquals("Error: 500 - { \"error\": \"Internal server error\" }", exception.getMessage());
    }

    @Test
    public void testGetGeneratedRubrics_noChoices() throws Exception {
        // Arrange
        String responseJson = "{ \"choices\": [] }";

        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(responseJson);

        JsonNode responseNode = new ObjectMapper().readTree(responseJson);
        when(mockObjectMapper.readTree(responseJson)).thenReturn(responseNode);
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("mockedRequestBodyJson");

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", "Generate rubrics for a mock patient scenario")
        );

        // Act
        JsonNode result = rubricsOpenAiClient.getGeneratedRubrics(messages);

        // Assert
        assertNull(result);
    }

    // Reflection utility to set private fields
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}