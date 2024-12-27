//package com.communication.communication_backend.service.facialAnalysis;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class FinalOutputConsumer {
//
//    private final List<String> gptResponses = new ArrayList<>();
//
//    @KafkaListener(topics = "${kafka.topics.gpt}", groupId = "final-output-group")
//    public void consume(String chatGPTResponse) {
//        synchronized (gptResponses) {
//            gptResponses.add(chatGPTResponse);
//        }
//        System.out.println("Received ChatGPT response: " + chatGPTResponse);
//    }
//
//    public void writeFinalOutput() {
//        synchronized (gptResponses) {
//            try (FileWriter writer = new FileWriter("final_output.txt", false)) { // Overwrite mode
//                for (String response : gptResponses) {
//                    writer.write(response + System.lineSeparator());
//                }
//                System.out.println("Final output written to final_output.txt");
//            } catch (IOException e) {
//                System.err.println("Error writing to final_output.txt: " + e.getMessage());
//            }
//        }
//    }
//}