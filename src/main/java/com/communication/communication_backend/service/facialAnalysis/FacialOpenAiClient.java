package com.communication.communication_backend.service.facialAnalysis;

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
public class FacialOpenAiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${chatgpt.api.url}")
    private String API_URL;

    @Value("${chatgpt.api.key}")
    private String apiKey;

    public FacialOpenAiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public FacialOpenAi getEmpathyRating(String chunk) throws Exception {
        String prompt = "The following is a chunk of information that is part of an output from an AI model " +
                "that detects emotions from a video of a medical student when talking to a patient. " +
                "You will notice there are three lines which state the top three emotions detected followed by " +
                "the probability of that student showing that emotion. Based on this, I want you to give a rating " +
                "of the student’s professionalism either it is 'good' or 'bad', especially with regards to the student’s level of empathy. " +
                "Please only give the rating and a summarized reasoning in the format of Rating: followed by Rationale: \n\n" +
                chunk;

        // Define the expected JSON schema for the response
        Map<String, Object> jsonSchema = Map.of(
                "name", "rating_schema",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "rating", Map.of(
                                        "type", "string",
                                        "enum", List.of("good", "bad"),
                                        "description", "The rating indicating the assessment of the professionalism."
                                ),
                                "reasoning", Map.of(
                                        "type", "string",
                                        "description", "A summarized reasoning for the given rating."
                                )
                        ),
                        "required", List.of("rating", "reasoning"),
                        "additionalProperties", false
                )
        );

        // Construct the request body
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a professional emotion evaluator."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", jsonSchema
                )
        );

        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                // The assistant's response is a JSON string matching the schema
                String contentJson = (String) message.get("content");
//                System.out.println(contentJson);
                JsonNode contentNode = objectMapper.readTree(contentJson); // debug this line
                if (contentNode.has("rating") && contentNode.has("reasoning")) {
                    String rating = contentNode.get("rating").asText();
                    String reasoning = contentNode.get("reasoning").asText();
                    return new FacialOpenAi(rating, reasoning);
                }
            }
        } else {
            throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
        }

        return new FacialOpenAi("null", "null");
    }
}