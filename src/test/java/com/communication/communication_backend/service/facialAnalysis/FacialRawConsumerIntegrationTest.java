// package com.communication.communication_backend.service.facialAnalysis;

// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.context.annotation.Import;
// import org.springframework.kafka.core.ConsumerFactory;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.testcontainers.containers.KafkaContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
// import org.testcontainers.utility.DockerImageName;

// import java.util.List;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;

// @SpringBootTest(classes = {FacialRawConsumer.class, FacialAnalysisKafkaTopicName.class})
// @Import(org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class)
// @Testcontainers
// public class FacialRawConsumerIntegrationTest {

//     @Container
//     static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

//     @Autowired
//     private KafkaTemplate<String, String> kafkaTemplate;

//     @Autowired
//     private ConsumerFactory<String, String> consumerFactory;

//     @DynamicPropertySource
//     static void kafkaProperties(DynamicPropertyRegistry registry) {
//         registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
//         registry.add("spring.kafka.consumer.group-id", () -> "test-group");
//         registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
//     }

//     @BeforeAll
//     static void setup() {
//         kafkaContainer.start();
//     }

//     @Test
//     public void testConsumeProcessesMessageAndSendsToRankedTopic() throws Exception {
//         FacialAnalysisKafkaTopicName topicName = new FacialAnalysisKafkaTopicName("2025-03-16", "testUser");
//         FacialRawConsumer consumer = new FacialRawConsumer(topicName, kafkaTemplate, consumerFactory);

//         String inputMessage = """
//                 {
//                   "face": {
//                     "predictions": [
//                       {
//                         "emotions": [
//                           {"name": "Joy", "score": 0.9},
//                           {"name": "Sadness", "score": 0.4},
//                           {"name": "Anger", "score": 0.6}
//                         ]
//                       }
//                     ]
//                   }
//                 }
//                 """;

//         kafkaTemplate.send(topicName.getHumeFace(), inputMessage).get();
//         Thread.sleep(2000); // Temporary; improve with consumer poll later

//         List<String> processedEmotions = consumer.processPrediction(inputMessage);
//         assertNotNull(processedEmotions);
//         assertEquals(3, processedEmotions.size());
//         assertEquals("Joy: 0.90", processedEmotions.get(0));
//     }
// }