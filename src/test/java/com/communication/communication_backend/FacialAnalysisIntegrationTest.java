// package com.communication.communication_backend;

// import org.apache.kafka.clients.consumer.ConsumerRecord;
// import org.apache.kafka.clients.producer.ProducerRecord;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.kafka.listener.MessageListener;
// import org.springframework.kafka.listener.KafkaMessageListenerContainer;
// import org.springframework.kafka.test.context.EmbeddedKafka;
// import org.springframework.kafka.test.utils.KafkaTestUtils;
// import org.springframework.test.context.junit.jupiter.SpringExtension;

// import java.util.concurrent.BlockingQueue;
// import java.util.concurrent.LinkedBlockingQueue;
// import java.util.concurrent.TimeUnit;

// import static org.assertj.core.api.Assertions.assertThat;

// @ExtendWith(SpringExtension.class)
// @SpringBootTest
// @EmbeddedKafka(partitions = 1, topics = {"facial-analysis-input", "facial-analysis-output"})
// class FacialAnalysisIntegrationTest {

//     @Autowired
//     private KafkaTemplate<String, String> kafkaTemplate;

//     @Autowired
//     private KafkaMessageListenerContainer<String, String> listenerContainer;

//     @Test
//     void testFacialAnalysisProcessing() throws InterruptedException {
//         // Create a blocking queue to collect messages
//         BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();

//         // Correctly set up the message listener
//         listenerContainer.setupMessageListener((MessageListener<String, String>) record -> records.add(record));

//         // Sending a sample JSON payload to the input topic
//         String inputMessage = "{\"imageData\": \"base64EncodedString\"}";
//         kafkaTemplate.send(new ProducerRecord<>("facial-analysis-input", inputMessage));

//         // Waiting for the processed message
//         ConsumerRecord<String, String> received = records.poll(10, TimeUnit.SECONDS);
        
//         assertThat(received).isNotNull();
//         assertThat(received.topic()).isEqualTo("facial-analysis-output");
//         assertThat(received.value()).contains("aiRating"); // Assuming AI rating is included in response
//     }
// }
