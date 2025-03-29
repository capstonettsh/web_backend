package com.communication.communication_backend.service.toneAnalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ExchangesConsumerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;

    @Mock
    private ConsumerFactory<String, String> consumerFactory;

    @Mock
    private KafkaMessageListenerContainer<String, String> container;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private FinalOpenAiClient finalOpenAiClient;

    @InjectMocks
    private ExchangesConsumer exchangesConsumer;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();

        // Mock Kafka topic names
        when(toneAnalysisKafkaTopicName.getHumeSpeechExchange()).thenReturn("test-hume-speech-exchange");
        when(toneAnalysisKafkaTopicName.getHumeSpeechGptResponse()).thenReturn("test-hume-speech-gpt-response");
        when(toneAnalysisKafkaTopicName.getOverallFeedback()).thenReturn("test-overall-feedback");

        // Mock container creation
        when(consumerFactory.createContainer(any())).thenReturn(container);
        exchangesConsumer = new ExchangesConsumer(toneAnalysisKafkaTopicName, kafkaTemplate, consumerFactory);
        
        // Inject mocks manually since @InjectMocks doesn't handle @Autowired fields
        try (MockedConstruction<OpenAiClient> openAiMock = mockConstruction(OpenAiClient.class)) {
            try (MockedConstruction<FinalOpenAiClient> finalOpenAiMock = mockConstruction(FinalOpenAiClient.class)) {
                exchangesConsumer = new ExchangesConsumer(toneAnalysisKafkaTopicName, kafkaTemplate, consumerFactory);
                this.openAiClient = openAiMock.constructed().get(0);
                this.finalOpenAiClient = finalOpenAiMock.constructed().get(0);
            }
        }
    }

    @Test
    public void testConsume_SuccessfulProcessing() throws Exception {
        // Arrange
        String exchangeJson = "{\"userMessage\": \"I feel unwell.\", \"assistantMessage\": \"I'm sorry to hear that.\"}";
        when(openAiClient.getEmpathyRating(exchangeJson)).thenReturn("8");

        // Act
        exchangesConsumer.consume(exchangeJson);

        // Assert
        JsonNode exchangeNode = objectMapper.readTree(exchangeJson);
        ObjectNode expectedNode = (ObjectNode) exchangeNode;
        expectedNode.put("rating", "8");
        String expectedUpdatedExchange = objectMapper.writeValueAsString(expectedNode);

        verify(openAiClient, times(1)).getEmpathyRating(exchangeJson);
        verify(kafkaTemplate, times(1)).send("test-hume-speech-gpt-response", expectedUpdatedExchange);
        assertFalse(exchangesConsumer.jsonNodeQueue.isEmpty()); // Queue should have the original exchange
    }

    @Test
    public void testConsume_OpenAiClientException() throws Exception {
        // Arrange
        String exchangeJson = "{\"userMessage\": \"I feel unwell.\", \"assistantMessage\": \"I'm sorry to hear that.\"}";
        when(openAiClient.getEmpathyRating(exchangeJson)).thenThrow(new RuntimeException("API Error"));

        // Act
        exchangesConsumer.consume(exchangeJson);

        // Assert
        verify(openAiClient, times(1)).getEmpathyRating(exchangeJson);
        verify(kafkaTemplate, never()).send(anyString(), anyString()); // No message sent due to exception
        assertFalse(exchangesConsumer.jsonNodeQueue.isEmpty()); // Queue should still have the exchange
    }

    @Test
    public void testEndChat_SuccessfulFeedback() throws Exception {
        // Arrange
        String exchangeJson = "{\"userMessage\": \"I feel unwell.\", \"assistantMessage\": \"I'm sorry to hear that.\"}";
        exchangesConsumer.consume(exchangeJson); // Add to queue

        JsonNode feedbackNode = objectMapper.readTree("{\"overallFeedback\": \"Good empathy shown\", \"top3Mistakes\": []}");
        when(finalOpenAiClient.getOverallFeedback(anyList(), eq("123"))).thenReturn(feedbackNode);

        // Act
        JsonNode result = exchangesConsumer.endChat("123");

        // Assert
        assertNotNull(result);
        assertEquals("Good empathy shown", result.get("overallFeedback").asText());
        verify(finalOpenAiClient, times(1)).getOverallFeedback(anyList(), eq("123"));
        verify(kafkaTemplate, times(1)).send("test-overall-feedback", objectMapper.writeValueAsString(feedbackNode));
        assertTrue(exchangesConsumer.jsonNodeQueue.isEmpty()); // Queue should be cleared
    }

    @Test
    public void testEndChat_EmptyQueue() {
        // Act
        JsonNode result = exchangesConsumer.endChat("123");

        // Assert
        assertNull(result);
        verify(finalOpenAiClient, never()).getOverallFeedback(anyList(), anyString());
        verify(kafkaTemplate, never()).send(eq("test-overall-feedback"), anyString());
    }

    @Test
    public void testEndChat_FeedbackException() throws Exception {
        // Arrange
        String exchangeJson = "{\"userMessage\": \"I feel unwell.\", \"assistantMessage\": \"I'm sorry to hear that.\"}";
        exchangesConsumer.consume(exchangeJson); // Add to queue
        when(finalOpenAiClient.getOverallFeedback(anyList(), eq("123"))).thenThrow(new RuntimeException("Feedback Error"));

        // Act
        JsonNode result = exchangesConsumer.endChat("123");

        // Assert
        assertNull(result);
        verify(finalOpenAiClient, times(1)).getOverallFeedback(anyList(), eq("123"));
        verify(kafkaTemplate, never()).send(eq("test-overall-feedback"), anyString());
        assertFalse(exchangesConsumer.jsonNodeQueue.isEmpty()); // Queue should not be cleared on error
    }
}

// import org.junit.jupiter.api.Test;
// import static org.assertj.core.api.Assertions.assertThat;

// class ToneAnalysisIntegrationTest {

//     @Test
//     void testConsume_SuccessfulProcessing() {
//         assertThat(true).isTrue();
//     }


//     @Test
//     void testConsume_OpenAiClientException() {
//         assertThat(true).isTrue();
//     }

//     @Test
//     void testEndChat_SuccessfulFeedback() {
//         assertThat(true).isTrue();
//     }

//     @Test
//     void testEndChat_EmptyQueue() {
//         assertThat(true).isTrue();
//     }

//     @Test
//     void testEndChat_FeedbackException() {
//         assertThat(true).isTrue();
//     }
// }

