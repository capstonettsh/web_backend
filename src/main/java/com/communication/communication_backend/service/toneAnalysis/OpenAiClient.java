package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    @Value("${chatgpt.api.url}")
    private String API_URL;
    @Value("${chatgpt.api.key}")
    private String apiKey;

    public OpenAiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String getEmpathyRating(String conversation) throws Exception {
        String prompt = "Evaluate the empathy level in the following conversation:\n\n" +
                conversation +
                "\n\nProvide a rating between 1 and 10, with 10 being highly empathetic.";

        int[] possibleRating = new int[11]; // 11 elements (0 to 10)

        // Fill the array with numbers from 0 to 10
        for (int i = 0; i < possibleRating.length; i++) {
            possibleRating[i] = i;
        }

        Map<String, Object> jsonSchema = Map.of(
                "name", "rating",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "value", Map.of(
                                        "type", "number",
                                        "description", "The integer value of the rating.",
                                        "enum", possibleRating
                                )
                        ),
                        "required", List.of("value"),
                        "additionalProperties", false
                )
        );

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an empathy evaluator."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", jsonSchema
                ),
                "max_tokens", 50
        );

        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        System.out.println("chat gpt 1");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("chat gpt 2");
        if (response.statusCode() == 200) {
            System.out.println("chat gpt 3");
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            System.out.println("chat gpt 4");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            System.out.println("chat gpt 5");
            if (!choices.isEmpty()) {
                System.out.println("chat gpt 6");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                // Assuming "content" is a JSON string here
                String contentJson = (String) message.get("content");
                JsonNode contentNode = objectMapper.readTree(contentJson);

                if (contentNode.has("value")) {
                    return String.valueOf(contentNode.get("value").asInt());
                }
            }
        } else {
            throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
        }

        return "No rating";
    }
}
