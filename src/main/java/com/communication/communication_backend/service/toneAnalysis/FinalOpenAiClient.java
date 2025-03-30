package com.communication.communication_backend.service.toneAnalysis;

import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import com.communication.communication_backend.repository.MarkingSchemaRepository;
import com.communication.communication_backend.repository.ScenarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinalOpenAiClient {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${chatgpt.api.url}")
  private String API_URL; // e.g. "https://api.openai.com/v1/responses"
  @Value("${chatgpt.api.key}")
  private String apiKey;

  @Autowired
  private MarkingSchemaRepository markingSchemaRepository;

  @Autowired
  private ScenarioRepository scenarioRepository;

  public FinalOpenAiClient() {
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  private String buildMarkingRubricDescription(String scenarioId) throws Exception {
    int scenarioIdInt = Integer.parseInt(scenarioId);

    // Fetch the scenario by ID
    Scenario scenario = scenarioRepository.findById(scenarioIdInt)
        .orElseThrow(() -> new Exception("Scenario not found for id: " + scenarioId));

    // Get the marking schemas for the scenario
    List<MarkingSchema> markingSchemas = markingSchemaRepository.findByScenario(scenario);

    StringBuilder rubricBuilder = new StringBuilder();
    for (MarkingSchema schema : markingSchemas) {
      rubricBuilder.append("Category: ").append(schema.getTitle()).append("\n");
      rubricBuilder.append("Description: ")
          .append("Unsatisfactory: ").append(schema.getUnsatisfactory()).append("\n")
          .append("Borderline:").append(schema.getBorderline()).append("\n")
          .append("Satisfactory:").append(schema.getSatisfactory())
          .append("\n");
    }
    return rubricBuilder.toString();
  }

  public JsonNode getOverallFeedback(List<Map<String, Object>> messages, String scenarioId) throws Exception {
    // 1. Build the dynamic rubric from DB
    String dynamicRubric = buildMarkingRubricDescription(scenarioId);
    System.out.println(dynamicRubric);

    // 2. Build the JSON schema that you'll place under "json_schema"
    Map<String, Object> jsonSchema = new HashMap<>();
    Map<String, Object> schema = new HashMap<>();
    jsonSchema.put("name", "feedback_schema");
    jsonSchema.put("strict", true);

    // Define the properties object
    Map<String, Object> properties = new HashMap<>();
    properties.put(
        "overallFeedback",
        Map.of(
            "type", "string",
            "description",
            "Please create a markdown table to evaluate a medical student's performance across key clinical categories." +
                "For each category, rate the performance as \"Unsatisfactory\", \"Borderline\", or \"Satisfactory\"." +
                "Additionally, provide specific, constructive feedback on how the student can improve in each category." + dynamicRubric
        )
    );
    properties.put(
        "top3Mistakes",
        Map.of(
            "type", "array",
            "description", "List of the top 3 mistakes that the user (doctor) made. "
                + "Include examples or suggestions for improvement.",
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
                        "description",
                        "Start time for the user when the mistake occurred."
                    ),
                    "userEndTime", Map.of(
                        "type", "integer",
                        "description",
                        "End time for the user when the mistake was acknowledged."
                    )
                ),
                "required", List.of("mistakeText", "exchangeRef", "mistakeReason", "userStartTime", "userEndTime"),
                "additionalProperties", false
            )
        )
    );

    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", List.of("overallFeedback", "top3Mistakes"));
    schema.put("additionalProperties", false);
    jsonSchema.put("schema", schema);

    // 3. Construct the response_format object
    //    (Note: You must have a `json_schema` key, not just `schema`.)
    Map<String, Object> responseFormat = new HashMap<>();
    responseFormat.put("type", "json_schema");

    responseFormat.put("json_schema", jsonSchema);

    // 4. Build your combined messages array
    List<Map<String, Object>> inputList = new ArrayList<>();

    // Developer message
    Map<String, Object> developerMessage = new HashMap<>();
    developerMessage.put("role", "developer");
    List<Map<String, Object>> developerContentList = new ArrayList<>();
    Map<String, Object> devContentItem = new HashMap<>();
    devContentItem.put("type", "text");
    devContentItem.put("text", "Formatting re-enabled");
    developerContentList.add(devContentItem);
    developerMessage.put("content", developerContentList);

    // Add the developer message first
    inputList.add(developerMessage);

    // Then add your existing user/assistant messages
    inputList.addAll(messages);

    // 5. Construct the request body
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("model", "o1");
    requestBody.put("messages", inputList);

    // Put the responseFormat map as "response_format"
    requestBody.put("response_format", responseFormat);

    // You can include your other parameters here
    requestBody.put("reasoning_effort", "medium");

    // 6. Convert to JSON
    String requestBodyJson = objectMapper.writeValueAsString(requestBody);
    System.out.println("REQUEST BODY JSON:\n" + requestBodyJson);

    // 7. Send the HTTP request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(API_URL))  // example: "https://api.openai.com/v1/responses"
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("RESPONSE BODY:\n" + response.body());
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

    // If none found, return null or handle accordingly
    return null;
  }

}