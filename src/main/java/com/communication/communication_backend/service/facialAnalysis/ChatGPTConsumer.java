//package com.communication.communication_backend.service.facialAnalysis;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class ChatGPTConsumer {
//
//    private final FacialOpenAiClient facialOpenAiClient;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final List<String> gptResponses = new ArrayList<>();
//    private final List<String> chunksProcessed = new ArrayList<>();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    @Value("${kafka.topics.gpt}")
//    private String gptTopic;
//    @Value("${final.output.file:final_output.txt}")
//    private String finalOutputFile;
//
//    public ChatGPTConsumer(FacialOpenAiClient facialOpenAiClient, KafkaTemplate<String, String> kafkaTemplate) {
//        this.facialOpenAiClient = facialOpenAiClient;
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    @KafkaListener(topics = "${kafka.topics.exchanges}", groupId = "gpt-group")
//    public void consumeChunk(String chunk) {
//        try {
//            System.out.println("Processing chunk with ChatGPT: " + chunk);
//
//            // Send chunk to ChatGPT and get response
//            String gptResponse = facialOpenAiClient.getEmpathyRating(chunk);
//
//            // Extract "content" field from GPT response
//            String content = extractContentFromResponse(gptResponse);
//
//            // Append chunk-response pair to gptResponses
//            String chunkResponsePair = chunk.trim() + System.lineSeparator() + System.lineSeparator() + content.trim();
//            synchronized (gptResponses) {
//                gptResponses.add(chunkResponsePair);
//            }
//
//            // Send response to gpt-rated-emotions topic
//            kafkaTemplate.send(gptTopic, gptResponse);
//            System.out.println("Sent GPT response to topic: " + gptTopic);
//
//            // Write the final output if all chunks are processed
//            if (allChunksProcessed()) {
//                writeFinalOutput();
//            }
//
//        } catch (Exception e) {
//            System.err.println("Error processing chunk with ChatGPT: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private String extractContentFromResponse(String gptResponse) throws IOException {
//        JsonNode rootNode = objectMapper.readTree(gptResponse);
//        JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
//        return contentNode.asText();
//    }
//
//    private boolean allChunksProcessed() {
//        // Check if all chunks have been processed (logic depends on your tracking implementation)
//        synchronized (chunksProcessed) {
//            return !chunksProcessed.isEmpty() && gptResponses.size() == chunksProcessed.size();
//        }
//    }
//
//    private void writeFinalOutput() {
//        try (FileWriter writer = new FileWriter(finalOutputFile, false)) { // Overwrite mode
//            synchronized (gptResponses) {
//                for (String chunkResponsePair : gptResponses) {
//                    writer.write(chunkResponsePair + System.lineSeparator() + System.lineSeparator());
//                }
//            }
//            System.out.println("Final output written to " + finalOutputFile);
//        } catch (IOException e) {
//            System.err.println("Error writing final output: " + e.getMessage());
//        }
//    }
//}
