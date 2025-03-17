// package com.communication.communication_backend.service.overallFeedback;

// import com.communication.communication_backend.controller.ChatController;
// import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
// import com.communication.communication_backend.service.toneAnalysis.ToneAnalysisKafkaTopicName;
// import org.apache.kafka.clients.producer.ProducerRecord;
// import org.apache.kafka.common.serialization.StringDeserializer;
// import org.junit.jupiter.api.AfterAll;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.context.annotation.ComponentScan;
// import org.springframework.context.annotation.Import;
// import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.kafka.listener.ContainerProperties;
// import org.springframework.kafka.listener.KafkaMessageListenerContainer;
// import org.springframework.kafka.listener.MessageListener;
// import org.springframework.kafka.test.context.EmbeddedKafka;
// import org.springframework.kafka.test.utils.KafkaTestUtils;
// import org.springframework.test.context.junit.jupiter.SpringExtension;
// import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
// import org.testcontainers.containers.KafkaContainer;
// import org.testcontainers.utility.DockerImageName;
// import org.springframework.kafka.core.ConsumerFactory;

// import java.util.Map;
// import java.util.concurrent.BlockingQueue;
// import java.util.concurrent.LinkedBlockingQueue;
// import java.util.concurrent.TimeUnit;

// import static org.assertj.core.api.Assertions.assertThat;

// @ExtendWith(SpringExtension.class)
// @AutoConfigureMockMvc
// @Import(ChatController.class)
// @SpringBootTest(properties = {"spring.profiles.active=test", "spring.main.web-application-type=none"})
// @EmbeddedKafka(partitions = 1, controlledShutdown = false, topics = {"test-speech", "test-face"})
// class GptResponseConsumerIntegrationTest {

//     private static KafkaContainer kafkaContainer;

//     @Autowired
//     private KafkaTemplate<String, String> kafkaTemplate;

//     @Autowired
//     private ConsumerFactory<String, String> consumerFactory;

//     @MockBean
//     private ServletServerContainerFactoryBean webSocketContainer; 

//     @BeforeAll
//     static void setup() {
//         kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
//         kafkaContainer.start();
//     }

//     @AfterAll
//     static void teardown() {
//         kafkaContainer.stop();
//     }

//     @Test
//     void testConsumeSpeechAndFacialAnalysis() throws Exception {
//         ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName = new ToneAnalysisKafkaTopicName("2025-03-16T12:00:00", "user123");
//         FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName = new FacialAnalysisKafkaTopicName("2025-03-16T12:00:00", "user123");
//         OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName = new OverallFeedbackKafkaTopicName("session1", "user1");

//         GptResponseConsumer consumer = new GptResponseConsumer(
//                 toneAnalysisKafkaTopicName,
//                 facialAnalysisKafkaTopicName,
//                 overallFeedbackKafkaTopicName,
//                 kafkaTemplate,
//                 consumerFactory);

//         // Set up a consumer to capture output messages
//         BlockingQueue<String> records = new LinkedBlockingQueue<>();
//         Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", kafkaContainer.getBootstrapServers());
//         DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
//         ContainerProperties containerProperties = new ContainerProperties(overallFeedbackKafkaTopicName.getCombined());
//         KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
//         container.setupMessageListener((MessageListener<String, String>) record -> records.add(record.value()));
//         container.start();

//         // Send messages
//         String speechMessage = "{\"userBeginTime\": 1000, \"userEndTime\": 5000}";
//         String faceMessage = "{\"startTime\": 1500}";

//         kafkaTemplate.send(new ProducerRecord<>(toneAnalysisKafkaTopicName.getHumeSpeechGptResponse(), speechMessage));
//         kafkaTemplate.send(new ProducerRecord<>(facialAnalysisKafkaTopicName.getHumeFaceGPTResponse(), faceMessage));

//         // Verify output message
//         String output = records.poll(10, TimeUnit.SECONDS);
//         assertThat(output).contains("facialAnalysis");
//     }
// }
