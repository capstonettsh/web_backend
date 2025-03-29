package com.communication.communication_backend.service.creatingScenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RubricsOpenAiClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private RubricsOpenAiClient rubricsOpenAiClient;

    private ObjectMapper objectMapper;

    @Value("${chatgpt.api.url}")
    private String apiUrl = "https://api.openai.com/v1/chat/completions"; // Default value for testing

    @Value("${chatgpt.api.key}")
    private String apiKey = "test-api-key"; // Default value for testing

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();

        // Inject the API URL and key into the service since @Value won't work without Spring context
        ReflectionTestUtils.setField(rubricsOpenAiClient, "API_URL", apiUrl);
        ReflectionTestUtils.setField(rubricsOpenAiClient, "apiKey", apiKey);
    }

    @Test
    public void testGetGeneratedRubrics_Success() throws Exception {
        // Arrange
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", "You are assisting in generating a marking rubric schema."),
            Map.of("role", "user", "content", "Scenario Title: Test Scenario\nRubric Title: Communication Skills")
        );

        // Mock HTTP response
        String mockResponseBody = """
            {
                "choices": [{
                    "message": {
                        "content": "{\\"generatedRubrics\\": [{\\"title\\": \\"Communication Skills\\", \\"unsatisfactory\\": \\"Poor communication\\", \\"borderline\\": \\"Adequate communication\\", \\"satisfactory\\": \\"Excellent communication\\"}]}"
                    }
                }]
            }
            """;
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockResponseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        // Act
        JsonNode result = rubricsOpenAiClient.getGeneratedRubrics(messages);

        // Assert
        assertNotNull(result);
        assertTrue(result.has("generatedRubrics"));
        JsonNode rubric = result.get("generatedRubrics").get(0);
        assertEquals("Communication Skills", rubric.get("title").asText());
        assertEquals("Poor communication", rubric.get("unsatisfactory").asText());
        assertEquals("Adequate communication", rubric.get("borderline").asText());
        assertEquals("Excellent communication", rubric.get("satisfactory").asText());

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testGetGeneratedRubrics_ErrorResponse() throws Exception {
        // Arrange
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", "You are assisting in generating a marking rubric schema."),
            Map.of("role", "user", "content", "Scenario Title: Test Scenario\nRubric Title: Communication Skills")
        );

        // Mock HTTP error response
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\": \"Bad request\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            rubricsOpenAiClient.getGeneratedRubrics(messages);
        });
        assertEquals("Error: 400 - {\"error\": \"Bad request\"}", exception.getMessage());

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testGetGeneratedRubrics_ErrorResponse() throws Exception {
        // Arrange
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", "You are assisting in generating a marking rubric schema."),
            Map.of("role", "user", "content", "Scenario Title: Test Scenario\nRubric Title: Communication Skills")
        );

        // Mock HTTP response with empty choices
        String mockResponseBody = "{\"choices\": []}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockResponseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        // Act
        JsonNode result = rubricsOpenAiClient.getGeneratedRubrics(messages);

        // Assert
        assertNull(result); // Expecting null when no valid content is found

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}

// import org.junit.jupiter.api.Test;
// import static org.assertj.core.api.Assertions.assertThat;

// class CreateScenariosIntegrationTest {

//     @Test
//     void testGetGeneratedRubrics_ErrorResponse() {
//         assertThat(true).isTrue();
//     }

//     @Test
//     void testGetGeneratedRubrics_Success() {
//         assertThat(true).isTrue();
//     }
// }
