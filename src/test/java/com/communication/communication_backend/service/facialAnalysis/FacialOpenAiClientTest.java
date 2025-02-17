package com.communication.communication_backend.service.facialAnalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FacialOpenAiClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private FacialOpenAiClient facialOpenAiClient;

    private static final String SAMPLE_JSON_RESPONSE = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{\\"rating\\": \\"good\\", \\"reasoning\\": \\"The student showed empathy\\"}"
                        }
                    }
                ]
            }
            """;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Set up the FacialOpenAiClient with a mocked HttpClient
        facialOpenAiClient = new FacialOpenAiClient();
        setPrivateField(facialOpenAiClient, "httpClient", httpClient);
        setPrivateField(facialOpenAiClient, "API_URL", "http://fake-url.com/api");
        setPrivateField(facialOpenAiClient, "apiKey", "test-api-key");

        // Mock the behavior of httpClient.send()
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(SAMPLE_JSON_RESPONSE);
    }

    @Test
    public void testGetEmpathyRating() throws Exception {
        String chunk = "Example chunk information";
        FacialOpenAi result = facialOpenAiClient.getEmpathyRating(chunk);

        assertEquals("good", result.rating());
        assertEquals("The student showed empathy", result.reasoning());
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}