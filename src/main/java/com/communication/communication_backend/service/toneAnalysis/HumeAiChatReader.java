package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class HumeAiChatReader {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private String chatId;
    @Value("${humeai.api.key}")
    private String humeApiKey;

    public HumeAiChatReader(KafkaTemplate<String, String> kafkaTemplate, ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicName;
//        this.chatId = chatId;
//        getAllChatEvents();
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
        getAllChatEvents();
    }

    public void getAllChatEvents() {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        int pageNumber = 0;
        int pageSize = 10;
        int totalPages = 1; // Initialize to enter the loop

        while (pageNumber < totalPages) {
            // Construct query parameters
            String queryParams = String.format("page_size=%d&page_number=%d&ascending_order=true",
                    pageSize, pageNumber);

            // Encode query parameters to ensure URL safety
            String encodedParams = URLEncoder.encode(queryParams, StandardCharsets.UTF_8);

            // Build the URL with query parameters
            String url = String.format("https://api.hume.ai/v0/evi/chats/%s?%s",
                    URLEncoder.encode(chatId, StandardCharsets.UTF_8),
                    queryParams); // Already encoded parts

            // Build the GET request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Hume-Api-Key", humeApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            try {
                // Send the GET request and get the response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Check if the response status is 200 OK
                if (response.statusCode() == 200) {
                    String responseBody = response.body();

                    // Parse the JSON response
                    JsonNode rootNode = objectMapper.readTree(responseBody);

                    // Extract pagination information
                    totalPages = rootNode.path("total_pages").asInt(1);

                    // Extract events_page array
                    JsonNode eventsPageNode = rootNode.path("events_page");
                    if (eventsPageNode.isArray()) {
                        Iterator<JsonNode> elements = eventsPageNode.elements();
                        while (elements.hasNext()) {
                            JsonNode eventNode = elements.next();
                            // Convert the event node back to a JSON string
                            String eventJson = objectMapper.writeValueAsString(eventNode);
                            // Send the event JSON to the Kafka topic
                            kafkaTemplate.send(toneAnalysisKafkaTopicName.getHumeSpeech(), eventJson);
                            // Optional: Log the sent event
                            // logger.info("Sent event to Kafka: {}", eventJson);
                        }
                    } else {
                        // Handle the case where events_page is not an array
                        System.err.println("events_page is not an array.");
                    }

                    // Move to the next page
                    pageNumber++;
                } else {
                    // Handle non-successful HTTP responses
                    System.err.println("Failed to fetch chat events. HTTP Status: " + response.statusCode());
                    // Optionally, you can break the loop or retry based on status code
                    break;
                }

            } catch (IOException | InterruptedException e) {
                // Handle exceptions related to the HTTP request and JSON parsing
                System.err.println("Error occurred while fetching chat events: " + e.getMessage());
                // Restore interrupted state... if interrupted
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                // Optionally, you may choose to break or retry
                break;
            }
        }
    }
}
