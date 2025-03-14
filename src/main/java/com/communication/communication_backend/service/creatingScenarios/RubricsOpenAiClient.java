package com.communication.communication_backend.service.creatingScenarios;

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
public class RubricsOpenAiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${chatgpt.api.url}")
    private String API_URL;

    @Value("${chatgpt.api.key}")
    private String apiKey;

    public RubricsOpenAiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode getGeneratedRubrics(List<Map<String, Object>> messages) throws Exception {
        // Construct the response_format as per the curl command
        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_schema");

        Map<String, Object> jsonSchema = new HashMap<>();
        jsonSchema.put("name", "mistake_summary");
        jsonSchema.put("strict", true);

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        properties.put("generatedRubrics", Map.of(
                "type", "array",
                "description", "You are to help with generating a marking rubric schema to evaluate the performance of medical students in a training application that helps medical students to practise their communication skills. The users of this application are medical students. A trained doctor overseeing the communication training has given the title of a mock patient scenario, the description of the same mock patient scenario (and possibly a prompt as well), task instructions for that mock scenario and the title of a marking rubric schema are presented to you. Generate a marking rubric schema to evaluate the performance of medical students in a mock patient scenario given the information and the title of the marking rubric schema presented. The marking rubric schema will consist of 3 levels of performance: unsatisfactory, borderline and satisfactory.",
                "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "title", Map.of(
                                        "type", "string",
                                        "description", "The exact title of this marking rubric schema as presented to you. It should be short. Examples include Communication Skills, Empathy and Clinical Assessment."
                                ),
                                "unsatisfactory", Map.of(
                                        "type", "string",
                                        "description", "Provide a general description of an unsatisfactory performance of the medical student in the mock patient scenario according to the criteria stated by the title of the marking rubric schema you generated earlier. It should be clear and concise."
                                ),
                                "borderline", Map.of(
                                        "type", "string",
                                        "description", "Provide a general description of an borderline or passable performance of the medical student in the mock patient scenario according to the criteria stated by the title of the marking rubric schema you generated earlier. It should be clear and concise. It should be better than the unsatisfactory performance."
                                ),
                                "satisfactory", Map.of(
                                        "type", "string",
                                        "description", "Provide a general description of an satisfactory performance of the medical student in the mock patient scenario according to the criteria stated by the title of the marking rubric schema you generated earlier. It should be clear and concise. It should be greatly better than the borderline performance."
                                )
                        ),
                        "required", List.of("title", "unsatisfactory", "borderline", "satisfactory"),
                        "additionalProperties", false
                )
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("generatedRubrics"));
        schema.put("additionalProperties", false);

        jsonSchema.put("schema", schema);
        responseFormat.put("json_schema", jsonSchema);

        // Construct the request body as per the curl command
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o"); // Corrected model name
        requestBody.put("messages", messages);
        requestBody.put("response_format", responseFormat);
        requestBody.put("temperature", 1);
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
