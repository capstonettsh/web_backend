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
public class HumeAiAccessToken {

    @Value("${humeai.api.key}")
    private String humeApiKey;

    @Value("${humeai.secret.key}")
    private String humeSecretKey;

    private final String tokenUrl = "https://api.hume.ai/oauth2-cc/token";

    public String getAccessToken() {
        try {
            // Encode API key and secret key as Base64

            String credentials = humeApiKey + ":" + humeSecretKey;
            System.out.println(credentials);
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            // Create the HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // Build the POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(tokenUrl))
                    .header("Authorization", "Basic " + encodedCredentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response);

            // Parse the JSON response to extract the access token
            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
//                JSONObject jsonResponse = new JSONObject(response.body());
                return (String) responseBody.get("access_token");
            } else {
                throw new RuntimeException("Failed to fetch access token. HTTP Status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while fetching access token", e);
        }
    }
}
