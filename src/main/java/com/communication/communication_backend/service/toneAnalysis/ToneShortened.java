package com.communication.communication_backend.service.toneAnalysis;

import java.util.Map;

public record ToneShortened(String role, String content, Map<String, Double> messageEmotions) {
}
