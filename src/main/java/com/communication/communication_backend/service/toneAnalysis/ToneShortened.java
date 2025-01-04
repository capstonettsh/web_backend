package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public record ToneShortened(String role, String content, Map<String, Double> messageEmotions, JsonNode jsonNode) {
}
