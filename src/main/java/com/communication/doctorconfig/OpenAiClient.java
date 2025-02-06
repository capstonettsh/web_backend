package com.communication.doctorconfig;

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

    public JsonNode getGeneratedResponse(List<Map<String, Object>> messages) throws Exception {
        // Construct the response_format as per the curl command
        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_schema");

        Map<String, Object> jsonSchema = new HashMap<>();
        jsonSchema.put("name", "mistake_summary");
        jsonSchema.put("strict", true);

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        properties.put("generatedScenario", Map.of(
                "type", "array",
                "description", "You are to help with generating a mock patient scenario in a training application that helps medical students to practise their communication skills. A trained doctor overseeing the communication training has given the title and the description of a mock patient scenario that is presented to you. Generate features of a consistent mock patient scenario given the title and description.",
                "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "summarisedScenario", Map.of(
                                        "type", "string",
                                        "description", "Summarised description of scenario to be seen by the medical student taking the training."
                                ),
                                "taskInstructions", Map.of(
                                        "type", "string",
                                        "description", "Instructions for the medical student on this mock patient scenario."
                                ),
                                "backgroundInfo", Map.of(
                                        "type", "string",
                                        "description", "Background information on the patient."
                                ),
                                "personalityTraits", Map.of(
                                        "type", "string",
                                        "description", "Description of the personality traits of the mock patient in the mock scenario."
                                ),
                                "openingLine", Map.of(
                                        "type", "string",
                                        "description", "The first line the mock patient says to the medical student."
                                ),
                                "questionsForDoctor", Map.of(
                                        "type", "string",
                                        "description", "Questions that the mock patient could ask the medical student. Ensure that each suggested question is distinct."
                                ),
                                "responseGuidelines", Map.of(
                                        "type", "string",
                                        "description", "Guidelines on how the mock patient should respond to the medical student."
                                ),
                                "sampleResponse", Map.of(
                                        "type", "string",
                                        "description", "Sample responses that the mock patient could give to the medical student."
                                )
                                // "Rubrics", Map.of(
                                //         "type", "array",
                                //         "description", "This is the rubric that the medical student will be assessed against.",
                                //         "items", Map.of(
                                //                 "type", "object",
                                //                 "properties", Map.of(
                                //                         "rubricCriteria1", Map.of(
                                //                                 "type", "string",
                                //                                 "description", "The rubric criteria."
                                //                         ),
                                //                         "rubricLevels", Map.of(
                                //                                 "type", "integer",
                                //                                 "description", "The score for the rubric."
                                //                         )
                                //                 ),
                                //                 "required", List.of("rubricText", "rubricScore"),
                                //                 "additionalProperties", false
                                //         )
                                // )
                        ),
                        "required", List.of("mistakeText", "exchangeRef", "mistakeReason", "userStartTime", "userEndTime"),
                        "additionalProperties", false
                )
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("Scenario"));
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
