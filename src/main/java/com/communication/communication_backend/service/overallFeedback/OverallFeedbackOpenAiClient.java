package com.communication.communication_backend.service.overallFeedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OverallFeedbackOpenAiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${chatgpt.api.url}")
    private String API_URL;

    @Value("${chatgpt.api.key}")
    private String apiKey;

    public OverallFeedbackOpenAiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends the collected chat messages to OpenAI's API and retrieves the overall feedback.
     *
     * @param messages List of messages containing role and content.
     * @return JsonNode representing the mistake summary.
     * @throws Exception if there's an error during the API call.
     */
    public JsonNode getOverallFeedback(List<Map<String, Object>> messages) throws Exception {
        // Construct the response_format as per the curl command
        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_schema");

        Map<String, Object> jsonSchema = new HashMap<>();
        jsonSchema.put("name", "mistake_summary");
        jsonSchema.put("strict", true);

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("overallFeedback", Map.of(
                "type", "string",
                "description", "Overall feedback for the whole conversation."
        ));
        properties.put("top3Mistakes", Map.of(
                "type", "array",
                "description", "List of the top 3 mistakes.",
                "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "mistakeText", Map.of(
                                        "type", "string",
                                        "description", "Description of the mistake."
                                ),
                                "exchangeRef", Map.of(
                                        "type", "integer",
                                        "description", "Reference ID for the exchange."
                                ),
                                "mistakeReason", Map.of(
                                        "type", "string",
                                        "description", "Reason for the mistake."
                                ),
                                "userStartTime", Map.of(
                                        "type", "integer",
                                        "description", "Start time for the user when the mistake occurred."
                                ),
                                "userEndTime", Map.of(
                                        "type", "integer",
                                        "description", "End time for the user when the mistake was acknowledged."
                                )
                        ),
                        "required", List.of("mistakeText", "exchangeRef", "mistakeReason", "userStartTime", "userEndTime"),
                        "additionalProperties", false
                )
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("overallFeedback", "top3Mistakes"));
        schema.put("additionalProperties", false);

        jsonSchema.put("schema", schema);
        responseFormat.put("json_schema", jsonSchema);

        // Construct the request body as per the curl command
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o"); // Corrected model name
        requestBody.put("messages", messages);
        requestBody.put("response_format", responseFormat);
        requestBody.put("temperature", 1);
        requestBody.put("max_completion_tokens", 2048);
        requestBody.put("top_p", 1);
        requestBody.put("frequency_penalty", 0);
        requestBody.put("presence_penalty", 0);

        // Serialize the request body to JSON
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        System.out.println("check request body json" + requestBodyJson);

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check if the response status is OK
        if (response.statusCode() == 200) {
            JsonNode responseBody = objectMapper.readTree(response.body());
            JsonNode choices = responseBody.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                if (message != null && message.has("content")) {
                    String contentJson = message.get("content").asText();
                    // Parse the content JSON to JsonNode
                    JsonNode contentNode = objectMapper.readTree(contentJson);
                    return contentNode;
                }
            }
        } else {
            // Handle error responses
            throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
        }

        // Return null if no valid response is found
        return null;
    }
}