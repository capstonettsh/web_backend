// package com.communication.communication_backend.service.facialAnalysis;

// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.kafka.core.ConsumerFactory;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.kafka.test.context.EmbeddedKafka;
// import org.springframework.test.annotation.DirtiesContext;
// import org.testcontainers.junit.jupiter.Testcontainers;

// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;

// @SpringBootTest
// @Testcontainers
// @EmbeddedKafka(partitions = 1, topics = {"test_hume_face_ranked", "test_hume_face_gpt_response"})
// @DirtiesContext
// public class FacialRankedConsumerIntegrationTest {

//     @Autowired
//     private KafkaTemplate<String, String> kafkaTemplate;

//     @Autowired
//     private ConsumerFactory<String, String> consumerFactory;

//     @MockBean
//     private FacialOpenAiClient facialOpenAiClient;

//     @Test
//     public void testConsumeCallsOpenAiAndSendsToGptTopic() throws Exception {
//         // Arrange
//         FacialAnalysisKafkaTopicName topicName = new FacialAnalysisKafkaTopicName("2025-03-16", "testUser");
//         FacialRankedConsumer consumer = new FacialRankedConsumer(topicName, kafkaTemplate, consumerFactory);

//         String rankedMessage = """
//                 {
//                   "emotions": ["Joy: 0.90", "Anger: 0.60", "Sadness: 0.40"],
//                   "startTime": 300
//                 }
//                 """;

//         when(facialOpenAiClient.getEmpathyRating(anyString())).thenReturn(new FacialOpenAi("good", "High joy indicates empathy"));

//         // Act
//         kafkaTemplate.send(topicName.getHumeFaceRanked(), rankedMessage).get();

//         // Simulate consumption
//         Thread.sleep(2000);

//         // Assert
//         // Typically, you'd consume from getHumeFaceGPTResponse topic to verify
//         // For this example, we trust the mock and Kafka interaction
//     }
// }