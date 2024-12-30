package com.communication.communication_backend.service;

import com.communication.communication_backend.config.AwsConfig;
import com.communication.communication_backend.service.facialAnalysis.*;
import com.communication.communication_backend.service.toneAnalysis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

@Service
public class StreamingWebSocketHandler extends BinaryWebSocketHandler {
    private final String userId = "1";
    @Autowired
    private ToneAnalysisKafkaTopicNameFactory toneAnalysisKafkaTopicNameFactory;
    private HumeAIAudioWebSocketClient humeAudioClient;
    private ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private ByteArrayOutputStream audioData;
    private ShortenedConsumer shortenedConsumer;
    @Autowired
    private FacialAnalysisKafkaTopicNameFactory facialAnalysisKafkaTopicNameFactory;
    //    private HumeAIExpressionManagementWebSocketClient humeAIExpressionManagementWebSocketClient;
    private FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private ByteArrayOutputStream videoData;
    private HumeAIExpressionManagementWebSocketClient humeAIExpressionManagementWebSocketClient;
    private String sessionDateTime;
    @Autowired
    private ApplicationContext context;

    @Value("${humeai.api.key}")
    private String humeAiApiKey;

    @Autowired
    private AwsConfig awsConfig;

    @Autowired
    private S3Client s3Client;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        URI uri = session.getUri();
        System.out.println(uri);
        sessionDateTime = null;

        if (uri != null) {
            UriComponents uriComponents = UriComponentsBuilder.fromUri(uri).build();
            Map<String, String> queryParams = uriComponents.getQueryParams().toSingleValueMap();
            sessionDateTime = URLDecoder.decode(queryParams.get("sessionDateTime"));
            sessionDateTime = sessionDateTime.replace(":", "-");
        }

        if (sessionDateTime != null) {
            System.out.println("SessionDateTime received: " + sessionDateTime);
        } else {
            // Handle missing 'sessionDateTime' parameter
        }

        // configure tone analysis
        audioData = new ByteArrayOutputStream();

        String userId = "1";

        URI humeUri = new URI("wss://api.hume.ai/v0/evi/chat?api_key=" + humeAiApiKey + "&config_id=cd796b48-001c-4b9e-a813-bbd079c9d20d");

        toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicNameFactory.create(sessionDateTime, userId);
        humeAudioClient = context.getBean(HumeAIAudioWebSocketClient.class, humeUri, session,
                toneAnalysisKafkaTopicName);


        humeAudioClient.connectBlocking();

        // configure facial analysis
        URI humeExpressionManagementUri = new URI("wss://api.hume.ai/v0/stream/models?api_key=" + humeAiApiKey);

        videoData = new ByteArrayOutputStream();

        facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicNameFactory.create(sessionDateTime, userId);
        humeAIExpressionManagementWebSocketClient = context.getBean(HumeAIExpressionManagementWebSocketClient.class, humeExpressionManagementUri, session, facialAnalysisKafkaTopicName);

        humeAIExpressionManagementWebSocketClient.connectBlocking();

        context.getBean(RawConsumer.class, toneAnalysisKafkaTopicName);
        shortenedConsumer = context.getBean(ShortenedConsumer.class, toneAnalysisKafkaTopicName);
        context.getBean(ExchangesConsumer.class, toneAnalysisKafkaTopicName);

        context.getBean(FacialRawConsumer.class, facialAnalysisKafkaTopicName);
        context.getBean(FacialRankedConsumer.class, facialAnalysisKafkaTopicName);


//        humeAIBatchProcessingClient = context.getBean(HumeAIBatchProcessingClient.class, humeAiApiKey);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer payload = message.getPayload();
        try {
            processMessage(session, payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processMessage(WebSocketSession session, ByteBuffer payload) throws IOException, InterruptedException {
        byte[] data = new byte[payload.remaining()];
        payload.get(data);

        if (data.length < 1) {
            System.err.println("Received empty message or message without type prefix.");
            return;
        }

        // First byte is the message type
        byte messageType = data[0];

        // The rest is the media data
        byte[] mediaData = new byte[data.length - 1];
        System.arraycopy(data, 1, mediaData, 0, mediaData.length);

        if (messageType == 0x01) {
            // Audio data
            processAudioData(session, mediaData);
        } else if (messageType == 0x02) {
            // Video data
            processVideoData(session, mediaData);
        } else if (messageType == 0x03) {
            // Screenshot image
            processImageData(session, mediaData);
        } else if (messageType == 0x04) {
            endChat();
        } else {
            System.err.println("Unknown message type received: " + messageType);
        }
    }

    private void processAudioData(WebSocketSession session, byte[] message) throws IOException {
        audioData.write(message);

        if (humeAudioClient != null && humeAudioClient.isOpen()) {
            humeAudioClient.sendAudioData(message);
        }
    }

    private void processVideoData(WebSocketSession session, byte[] message) throws IOException {
        videoData.write(message);
    }

    private void processImageData(WebSocketSession session, byte[] message) throws IOException {
        // Example processing: Save the image to a directory
        String imagePath = "received_images/" + sessionDateTime + "_" + System.currentTimeMillis() + ".jpg";
        Files.createDirectories(Paths.get("received_images"));
        try (FileOutputStream fos = new FileOutputStream(imagePath)) {
            fos.write(message);
        }
        humeAIExpressionManagementWebSocketClient.sendImage(Base64.getEncoder().encodeToString(message));

        // Add additional processing logic here
        // e.g., send to facial analysis service
    }

    private void endChat() throws IOException, InterruptedException {
        shortenedConsumer.endChat();

        String videoPath = "recorded_videos/" + sessionDateTime + "_" + System.currentTimeMillis() + ".webm";
        String audioPath = "recorded_audios/" + sessionDateTime + "_" + System.currentTimeMillis() + ".webm";
        String combinedPath = "recorded_combined/" + sessionDateTime + "_" + System.currentTimeMillis() + "_combined.webm";
        try {


            if (videoData.size() > 0 && audioData.size() > 0) {
                // Create directories
                Files.createDirectories(Paths.get("recorded_videos"));
                Files.createDirectories(Paths.get("recorded_audios"));
                Files.createDirectories(Paths.get("recorded_combined"));

                // Save video data to file

                try (FileOutputStream fos = new FileOutputStream(videoPath)) {
                    fos.write(videoData.toByteArray());
                }

                // Save audio data to file

                try (FileOutputStream fos = new FileOutputStream(audioPath)) {
                    fos.write(audioData.toByteArray());
                }

                // Path for the combined output


                // Combine audio and video using FFmpeg
                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg",
                        "-i", videoPath,
                        "-i", audioPath,
                        "-c", "copy",
                        combinedPath
                );
                Process process = pb.start();
                process.waitFor();

                String bucketName = awsConfig.getBucketName();
                long timestamp = System.currentTimeMillis();

                String sessionTimestamp = sessionDateTime + "_" + timestamp;
                byte[] combinedData = Files.readAllBytes(Paths.get(combinedPath));
                // Define S3 key for combined file
                String combinedKey = "recorded_combined/" + sessionTimestamp + "_combined.webm";
                // Upload combined file to S3
                uploadToS3(bucketName, combinedKey, combinedData);
            } else if (videoData.size() > 0) {
//                String videoPath = "recorded_videos/" + sessionDateTime + "_" + System.currentTimeMillis() + ".webm";
                Files.createDirectories(Paths.get("recorded_videos"));
                try (FileOutputStream fos = new FileOutputStream(videoPath)) {
                    fos.write(videoData.toByteArray());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Files.delete(Paths.get(videoPath));
            Files.delete(Paths.get(audioPath));
            Files.delete(Paths.get(combinedPath));
        }
    }

    private void uploadToS3(String bucketName, String key, byte[] data) {
        PutObjectRequest putObj = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(putObj, RequestBody.fromBytes(data));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        // Close Hume AI clients
        if (humeAudioClient != null && humeAudioClient.isOpen()) {
            humeAudioClient.close();
        }

//        if (humeAIBatchProcessingClient != null) {
//            humeAIBatchProcessingClient.shutdown();
//        }
    }
}