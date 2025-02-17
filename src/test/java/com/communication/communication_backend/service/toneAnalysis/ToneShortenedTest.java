package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToneShortenedTest {

    @Test
    void testToneShortenedCreation() throws Exception {
        // Arrange
        String role = "user";
        String content = "Hello, how are you?";
        Map<String, Double> messageEmotions = Map.of("happiness", 0.85, "sadness", 0.10);
        
        // Create a mock JsonNode
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree("{\"key\": \"value\"}");

        // Act
        ToneShortened toneShortened = new ToneShortened(role, content, messageEmotions, jsonNode);

        // Assert
        assertNotNull(toneShortened, "ToneShortened instance should not be null.");
        assertEquals(role, toneShortened.role(), "Role should match the expected value.");
        assertEquals(content, toneShortened.content(), "Content should match the expected value.");
        assertEquals(messageEmotions, toneShortened.messageEmotions(), "Message emotions should match.");
        assertEquals(jsonNode, toneShortened.jsonNode(), "JsonNode should match the expected value.");
    }
}
