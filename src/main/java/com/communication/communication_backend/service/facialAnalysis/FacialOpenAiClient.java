//package com.communication.communication_backend.service.facialAnalysis;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class FacialOpenAiClient {
//
//    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
//    @Value("${open.ai.key}")
//    private String apiKey;
//
//    public FacialOpenAiClient() {
//        System.out.println("OpenAiClient initialized with API Key: " + (apiKey != null ? "Present" : "Missing"));
//    }
//
//    public void testApiKey() {
//        System.out.println("API Key during method call: " + apiKey);
//    }
//
//    public String getEmpathyRating(String chunk) {
//        // Debug the API key value
//        System.out.println("API Key from @Value annotation: " + (apiKey != null ? "Present" : "Missing"));
//
//        // Attempt to debug using environment variables
//        String envApiKey = System.getenv("OPENAI_API_KEY");
//        System.out.println("API Key from environment variable: " + (envApiKey != null ? "Present" : "Missing"));
//
//        // Debug both values
//        if (apiKey == null || apiKey.isEmpty()) {
//            System.out.println("No API key found in @Value property. Attempting to use environment key.");
//            apiKey = envApiKey; // Fallback to environment variable
//        }
//
//        if (apiKey == null || apiKey.isEmpty()) {
//            throw new RuntimeException("OpenAI API key is missing. Check your configuration.");
//        }
//
//        // Build headers with Authorization and Content-Type
//        HttpHeaders headers = buildHeaders();
//
//        // Build JSON payload as a Map
//        Map<String, Object> payload = createRequestBody(chunk);
//
//        // Create the HTTP entity
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
//
//        // Make the POST request to OpenAI API
//        return makeApiCall(entity);
//    }
//
//    /**
//     * Builds HTTP headers including the Authorization header with the API key.
//     */
//    private HttpHeaders buildHeaders() {
//        // Debugging: Check if the API key is present
//        System.out.println("API Key: " + (apiKey != null ? "Present" : "Missing"));
//        System.out.println("Loaded API Key: " + apiKey);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(apiKey); // Automatically sets "Authorization: Bearer <apiKey>"
//        headers.set("Content-Type", "application/json");
//        return headers;
//    }
//
//    /**
//     * Constructs the request body for the OpenAI API call as a Map.
//     */
//    private Map<String, Object> createRequestBody(String chunk) {
//        String prompt = "The following is a chunk of information that is part of an output from an AI model "
//                + "that detects emotions from a video of a medical student when talking to a patient. "
//                + "You will notice there are three lines which state the top three emotions detected followed by "
//                + "the probability of that student showing that emotion. Based on this, I want you to give a rating "
//                + "of the student’s professionalism either it is 'good' or 'bad', especially with regards to the student’s level of empathy. "
//                + "Please only give the rating and a summarized reasoning in the format of Rating: followed by Rationale: \n\n" + chunk;
//
//        // Create the message structure required by OpenAI API
//        Map<String, Object> systemMessage = Map.of("role", "system", "content", "You are a professional emotion evaluator.");
//        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);
//
//        // Construct the payload
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", "gpt-4");
//        requestBody.put("messages", List.of(systemMessage, userMessage));
//        return requestBody;
//    }
//
//    /**
//     * Makes the API call to OpenAI and returns the response.
//     */
//    private String makeApiCall(HttpEntity<Map<String, Object>> entity) {
//        RestTemplate restTemplate = new RestTemplate();
//        try {
//            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);
//            return response.getBody();
//        } catch (Exception e) {
//            System.err.println("Error processing chunk with ChatGPT: " + e.getMessage());
//            throw new RuntimeException("Failed to fetch empathy rating from ChatGPT.", e);
//        }
//    }
//}
