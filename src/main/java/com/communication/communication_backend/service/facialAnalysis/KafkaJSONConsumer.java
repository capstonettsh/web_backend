//package com.communication.communication_backend.service.facialAnalysis;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//
//@Service
//public class KafkaJSONConsumer {
//
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    // Kafka topic for storing chunk-response pairs
//    private final String gptTopic = "gpt-rated-emotions";
//
//    public KafkaJSONConsumer(KafkaTemplate<String, String> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    @KafkaListener(topics = "facialemoreview", groupId = "emotion-group")
//    public void consume(String message) {
//        System.out.println("Consumed message: " + message);
//
//        try {
//            // Process JSON into predictions
//            List<String> processedOutput = RawConsumer.processPredictions(message);
//
//            // Write processed output to output.txt
//            writeToFile(processedOutput, "output.txt");
//            System.out.println("Processed output written to output.txt");
//
//            // Process JSON into chunks
//            List<String> chunks = RawConsumer.processPredictionsIntoChunks(message);
//
//            // Send each chunk to GPT API and store chunk-response pairs
//            for (String chunk : chunks) {
//                String gptResponse = sendChunkToGPT(chunk);
//                String pair = chunk + "\n\nResponse:\n" + gptResponse;
//                kafkaTemplate.send(gptTopic, pair);
//                System.out.println("Stored chunk-response pair in topic: " + gptTopic);
//            }
//        } catch (Exception e) {
//            System.err.println("Error processing message: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void writeToFile(List<String> data, String filePath) {
//        try (FileWriter writer = new FileWriter(filePath, false)) { // Overwrite mode
//            for (String line : data) {
//                writer.write(line + System.lineSeparator());
//            }
//        } catch (IOException e) {
//            System.err.println("Error writing to file: " + e.getMessage());
//        }
//    }
//
//    private String sendChunkToGPT(String chunk) {
//        // Call OpenAI client to get GPT response
//        OpenAiClient client = new OpenAiClient();
//        return client.getEmpathyRating(chunk);
//    }
//}