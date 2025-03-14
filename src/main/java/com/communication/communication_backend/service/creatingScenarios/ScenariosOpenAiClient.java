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
public class ScenariosOpenAiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${chatgpt.api.url}")
    private String API_URL;

    @Value("${chatgpt.api.key}")
    private String apiKey;

    public ScenariosOpenAiClient() {
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
                "description", "You are to help with generating a mock patient scenario in a training application that helps medical students to practise their communication skills. The users of this application are medical students. A trained doctor overseeing the communication training has given the title and the description of a mock patient scenario that is presented to you. Generate features of a consistent mock patient scenario given the title and description.",
                "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "taskInstructions", Map.of(
                                        "type", "string",
                                        "description", "Specific instructions for the medical student for this mock patient scenario on the information that the user should mention to the mock patient. It should be clear and concise."
                                ),
                                "backgroundInfo", Map.of(
                                        "type", "string",
                                        "description", "Background information on the mock patient."
                                ),
                                "personalityTraits", Map.of(
                                        "type", "string",
                                        "description", "Description of the personality traits of the mock patient in the mock scenario."
                                ),
                                "questionsForDoctor", Map.of(
                                        "type", "string",
                                        "description", "Questions that the mock patient could ask the medical student. Ensure that each suggested question is distinct."
                                ),
                                "responseGuidelines", Map.of(
                                        "type", "string",
                                        "description", "Guidelines on how the mock patient should respond to the medical student."
                                ),
                                "sampleResponses", Map.of(
                                        "type", "string",
                                        "description", "Sample responses that the mock patient could give to the medical student. It should be in a form of a JSON with the keys being certain keywords such as 'symptoms' and 'concerns' and the values being the one-to-two-lines response relevant to that keyword."
                                )
                        ),
                        "required", List.of("taskInstructions", "backgroundInfo", "personalityTraits", "questionsForDoctor", "responseGuidelines", "sampleResponses"),
                        "additionalProperties", false
                )
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("generatedScenario"));
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
        System.out.println(response.body());

        // Check if the response status is OK
        if (response.statusCode() == 200) {
            JsonNode responseBody = objectMapper.readTree(response.body());
            JsonNode choices = responseBody.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                if (message != null) {
                    JsonNode contentNode = message.get("content");
                    if (contentNode != null && !contentNode.isNull()) {
                        String contentJson = contentNode.asText();
                        // Parse the content JSON to JsonNode
                        JsonNode contentParsed = objectMapper.readTree(contentJson);
                        return contentParsed;
                    } else {
                        throw new RuntimeException("Missing 'content' in the message.");
                    }
                }
            }
            // Handle if choices are empty or message is missing
            throw new RuntimeException("No valid message found in the response.");
        } else {
            throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
        }
    }
}