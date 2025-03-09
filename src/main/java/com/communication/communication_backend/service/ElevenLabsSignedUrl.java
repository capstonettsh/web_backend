package com.communication.communication_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;

@Service
public class ElevenLabsSignedUrl {
    @Value("${elevenlabs.api.key}")
    private String elevenlabsApiKey;

    public String getSignedUrl(String agentId) {
        try {
            String apiUrl = "https://api.elevenlabs.io/v1/convai/conversation/get_signed_url?agent_id=" + agentId;
            // Create the HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // Build the POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("xi-api-key", elevenlabsApiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
//                JSONObject jsonResponse = new JSONObject(response.body());
                return (String) responseBody.get("signed_url");
            } else {
                throw new RuntimeException("Failed to fetch signed url. HTTP Status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while fetching signed url", e);
        }
    }
}
