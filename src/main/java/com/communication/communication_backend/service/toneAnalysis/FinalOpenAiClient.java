package com.communication.communication_backend.service.toneAnalysis;

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
public class FinalOpenAiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${chatgpt.api.url}")
    private String API_URL;

    @Value("${chatgpt.api.key}")
    private String apiKey;

    public FinalOpenAiClient() {
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
                "description", "Overall feedback for the whole conversation. Grading rubric: Clinical Communication Skills (C)\n" +
                        "Key Issues: Breaking the news in a clear, empathetic, and sensitive manner. Avoiding medical jargon and using language that the patient can understand.\n" +
                        "Satisfactory: Uses simple, clear language; explains the diagnosis empathetically and at a pace the patient can follow. Maintains eye contact and demonstrates active listening.\n" +
                        "Borderline: Uses some jargon but attempts to clarify terms. Breaks the news abruptly but shows some effort to console the patient. Limited engagement with the patient’s emotional cues.\n" +
                        "Unsatisfactory: Uses excessive jargon or is too blunt in delivering the diagnosis. Fails to acknowledge or address the patient’s emotional response. Shows little or no empathy.\n" +
                        "\n" +
                        "Managing Patient’s Concerns (F)\n" +
                        "\n" +
                        "Key Issues: Addressing patient’s questions and emotions effectively, including disbelief, fear, and concerns about the family’s future.\n" +
                        "\n" +
                        "Satisfactory: Acknowledges the patient’s shock and provides reassurance. Encourages the patient to express emotions and validates their concerns. Offers appropriate answers to questions about prognosis and next steps.\n" +
                        "\n" +
                        "Borderline: Addresses some concerns but misses key emotional cues (e.g., family impact). Reassurance is vague or inconsistent.\n" +
                        "\n" +
                        "Unsatisfactory: Avoids or dismisses patient’s concerns. Provides inadequate explanations or gives false hope. May become defensive or argumentative if the patient is distressed.\n" +
                        "\n" +
                        "Clinical Judgement (E)\n" +
                        "\n" +
                        "Key Issues: Providing accurate and honest information about the diagnosis and prognosis. Offering realistic next steps, including symptom management and palliative care options.\n" +
                        "\n" +
                        "Satisfactory: Clearly communicates the advanced nature of the cancer and the lack of curative options. Explains the role of palliative care in managing symptoms and quality of life.\n" +
                        "\n" +
                        "Borderline: Gives incomplete information about the diagnosis or prognosis. Lacks clarity when explaining symptom management or next steps.\n" +
                        "\n" +
                        "Unsatisfactory: Provides misleading or inaccurate information. Fails to discuss symptom management or palliative care appropriately.\n" +
                        "\n" +
                        "Maintaining Patient Welfare (G)\n" +
                        "\n" +
                        "Key Issues: Ensuring that the patient feels supported and respected during the consultation. Demonstrating professionalism and empathy throughout.\n" +
                        "\n" +
                        "Satisfactory: Maintains a compassionate and professional demeanor. Offers emotional support and reassures the patient that they will be cared for. Refers appropriately to palliative care or spiritual support services.\n" +
                        "\n" +
                        "Borderline: Shows professionalism but lacks consistent empathy. Emotional support is minimal or not tailored to the patient’s needs.\n" +
                        "\n" +
                        "Unsatisfactory: Appears rushed or disengaged. Shows little concern for the patient’s emotional well-being. Does not offer appropriate referrals or support services." +
                        "Please give the user appropriate thing to say or thing to do as example so that the user (doctor) can improve."
        ));
        properties.put("top3Mistakes", Map.of(
                "type", "array",
                "description", "List of the top 3 mistakes that user (doctor) made when talking to patient, and give some example or what they should do so that user (doctor) can improve.",
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
        requestBody.put("max_completion_tokens", 4048);
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