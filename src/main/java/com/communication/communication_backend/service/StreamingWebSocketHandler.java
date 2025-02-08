package com.communication.communication_backend.service;

import com.communication.communication_backend.config.AwsConfig;
import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicName;
import com.communication.communication_backend.service.facialAnalysis.FacialAnalysisKafkaTopicNameFactory;
import com.communication.communication_backend.service.facialAnalysis.FacialRawConsumer;
import com.communication.communication_backend.service.facialAnalysis.HumeAIExpressionManagementWebSocketClient;
import com.communication.communication_backend.service.overallFeedback.GptResponseConsumer;
import com.communication.communication_backend.service.overallFeedback.OverallFeedbackExchangesConsumer;
import com.communication.communication_backend.service.overallFeedback.OverallFeedbackKafkaTopicName;
import com.communication.communication_backend.service.overallFeedback.OverallFeedbackKafkaTopicNameFactory;
import com.communication.communication_backend.service.toneAnalysis.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class StreamingWebSocketHandler extends BinaryWebSocketHandler {
    private final String userId = "1";
    private WebSocketSession webSocketSession;
    @Autowired
    private ToneAnalysisKafkaTopicNameFactory toneAnalysisKafkaTopicNameFactory;
    //    private HumeAIAudioWebSocketClient humeAudioClient;
    private ToneAnalysisKafkaTopicName toneAnalysisKafkaTopicName;
    private ByteArrayOutputStream audioData;
    private ShortenedConsumer shortenedConsumer;
    @Autowired
    private FacialAnalysisKafkaTopicNameFactory facialAnalysisKafkaTopicNameFactory;
    //    private HumeAIExpressionManagementWebSocketClient humeAIExpressionManagementWebSocketClient;
    private FacialAnalysisKafkaTopicName facialAnalysisKafkaTopicName;
    private ByteArrayOutputStream videoData;
    private HumeAIExpressionManagementWebSocketClient humeAIExpressionManagementWebSocketClient;

    @Autowired
    private OverallFeedbackKafkaTopicNameFactory overallFeedbackKafkaTopicNameFactory;
    private OverallFeedbackKafkaTopicName overallFeedbackKafkaTopicName;
    private String sessionDateTime;

    private GptResponseConsumer gptResponseConsumer;
    private OverallFeedbackExchangesConsumer overallFeedbackExchangesConsumer;
    private ExchangesConsumer exchangesConsumer;
    @Autowired
    private ApplicationContext context;

    @Value("${humeai.api.key}")
    private String humeAiApiKey;

    @Autowired
    private AwsConfig awsConfig;

    @Autowired
    private S3Client s3Client;

    private JsonNode overallFeedback;

    private void downloadAudioFile(String signedAudioUrl, String outputFilePath) throws IOException, InterruptedException {
        // Create the HTTP request to download the audio file
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(signedAudioUrl))
                .GET()
                .build();

        // Send the request and get the response as an InputStream
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Check if the response is successful
        if (response.statusCode() == 200) {
            // Ensure the output directory exists
            Files.createDirectories(Paths.get(outputFilePath).getParent());

            // Save the audio file to the specified path
            try (InputStream inputStream = response.body();
                 FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } else {
            throw new IOException("Failed to download audio file. HTTP Status: " + response.statusCode());
        }
    }

    private String getSignedAudioUrl(String chatId) throws IOException, InterruptedException {
        // Build the URL for the Hume AI API
        String url = String.format("https://api.hume.ai/v0/evi/chats/%s/audio",
                URLEncoder.encode(chatId, StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();

        // Loop until we get a non-null signed_audio_url
        while (true) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Hume-Api-Key", humeAiApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                System.out.println(responseBody);
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

                // Check that the signed_audio_url key exists and its value is not null
                if (jsonObject.has("signed_audio_url")) {
                    if (!jsonObject.get("signed_audio_url").isJsonNull()) {
                        return jsonObject.get("signed_audio_url").getAsString();
                    }
                } else {
                    throw new IOException("signed_audio_url not found in the response");
                }
            } else {
                throw new IOException("Failed to fetch audio metadata. HTTP Status: " + response.statusCode());
            }
            // Wait for 500ms before trying again to avoid a tight loop.
            Thread.sleep(500);
        }
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        this.webSocketSession = session;

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
        URI humeExpressionManagementUri = new URI("wss://api.hume.ai/v0/stream/models?api_key=" + humeAiApiKey);

        toneAnalysisKafkaTopicName = toneAnalysisKafkaTopicNameFactory.create(sessionDateTime, userId);
        facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicNameFactory.create(sessionDateTime, userId);
        overallFeedbackKafkaTopicName = overallFeedbackKafkaTopicNameFactory.create(sessionDateTime, userId);

//        humeAudioClient = context.getBean(HumeAIAudioWebSocketClient.class, humeUri, session,
//                toneAnalysisKafkaTopicName);
//
//
//        humeAudioClient.connectBlocking();

        videoData = new ByteArrayOutputStream();

        facialAnalysisKafkaTopicName = facialAnalysisKafkaTopicNameFactory.create(sessionDateTime, userId);
        humeAIExpressionManagementWebSocketClient = context.getBean(HumeAIExpressionManagementWebSocketClient.class, humeExpressionManagementUri, session, facialAnalysisKafkaTopicName);

        humeAIExpressionManagementWebSocketClient.connectBlocking();

        context.getBean(RawConsumer.class, toneAnalysisKafkaTopicName);
        shortenedConsumer = context.getBean(ShortenedConsumer.class, toneAnalysisKafkaTopicName);
        this.exchangesConsumer = context.getBean(ExchangesConsumer.class, toneAnalysisKafkaTopicName);

        context.getBean(FacialRawConsumer.class, facialAnalysisKafkaTopicName);
//        context.getBean(FacialRankedConsumer.class, facialAnalysisKafkaTopicName);

        gptResponseConsumer = context.getBean(GptResponseConsumer.class, toneAnalysisKafkaTopicName, facialAnalysisKafkaTopicName, overallFeedbackKafkaTopicName);
        overallFeedbackExchangesConsumer = context.getBean(OverallFeedbackExchangesConsumer.class, toneAnalysisKafkaTopicName, overallFeedbackKafkaTopicName);
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
//            processAudioData(session, mediaData);
        } else if (messageType == 0x02) {
            // Video data
            processVideoData(session, mediaData);
        } else if (messageType == 0x03) {
            // Screenshot image
            processImageData(session, mediaData);
        } else if (messageType == 0x04) {
            // Ending signal with payload format: "chatId:msDifference"
            String payloadString = new String(mediaData, StandardCharsets.UTF_8);
            String[] parts = payloadString.split(":");
            if (parts.length != 2) {
                System.err.println("Invalid end chat message format. Expected 'chatId:msDifference', got: " + payloadString);
                return;
            }

            String chatId = parts[0];
            long msDifference;
            try {
                msDifference = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid millisecond difference value: " + parts[1]);
                return;
            }

            // Now you can use chatId and msDifference as needed.
            endChat(chatId, msDifference);
        } else {
            System.err.println("Unknown message type received: " + messageType);
        }
    }

    private void processAudioData(WebSocketSession session, byte[] message) throws IOException {
        audioData.write(message);

//        if (humeAudioClient != null && humeAudioClient.isOpen()) {
//            humeAudioClient.sendAudioData(message);
//        }
    }

    private void processVideoData(WebSocketSession session, byte[] message) throws IOException {
        videoData.write(message);
    }

    private void processImageData(WebSocketSession session, byte[] message) throws IOException {
        // Example processing: Save the image to a directory
//        String imagePath = "received_images/" + sessionDateTime + "_" + System.currentTimeMillis() + ".jpg";
//        Files.createDirectories(Paths.get("received_images"));
//        try (FileOutputStream fos = new FileOutputStream(imagePath)) {
//            fos.write(message);
//        }
        humeAIExpressionManagementWebSocketClient.sendImage(Base64.getEncoder().encodeToString(message));

        // Add additional processing logic here
        // e.g., send to facial analysis service
    }

    private void endChat(String chatId, long msDifference) throws IOException, InterruptedException {
        CompletableFuture.runAsync(() -> {
                    HumeAiChatReader humeAiChatReader = context.getBean(HumeAiChatReader.class, toneAnalysisKafkaTopicName);
                    humeAiChatReader.setChatId(chatId);
                })
                .thenRun(() -> shortenedConsumer.endChat())
                .thenRun(() -> {
                    try {
                        Thread.sleep(3000);
                        System.out.println("sleeping 5000 ms");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenRun(() -> this.overallFeedback = exchangesConsumer.endChat())
                .join();

        long currentTime = System.currentTimeMillis();
        String videoPath = "recorded_videos/" + sessionDateTime + "_" + currentTime + ".webm";
        String audioPath = "recorded_audios/" + sessionDateTime + "_" + currentTime + ".webm";
        String combinedPath = "recorded_combined/" + sessionDateTime + "_" + currentTime + "_combined.webm";
        try {
            if (videoData.size() > 0) {
                // Create directories
                Files.createDirectories(Paths.get("recorded_videos"));
                Files.createDirectories(Paths.get("recorded_audios"));
                Files.createDirectories(Paths.get("recorded_combined"));

                String signedAudioUrl = getSignedAudioUrl(chatId);
                downloadAudioFile(signedAudioUrl, audioPath);

                try (FileOutputStream fos = new FileOutputStream(videoPath)) {
                    fos.write(videoData.toByteArray());
                }

                double offsetSeconds = (double) (msDifference / 1000.0);

                // Combine audio and video using FFmpeg
                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg",
                        "-i", videoPath,
                        "-itsoffset", String.valueOf(offsetSeconds),
                        "-i", audioPath,
                        "-map", "0:v",         // use video from first input
                        "-map", "1:a",         // use audio from second input
                        "-c:v", "copy",
                        "-c:a", "libopus",
                        combinedPath
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Capture FFmpeg output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
                }

                String bucketName = awsConfig.getBucketName();

                String sessionTimestamp = sessionDateTime + "_" + currentTime;
                byte[] combinedData = Files.readAllBytes(Paths.get(combinedPath));
                // Define S3 key for combined file
                String combinedKey = "recorded_combined/" + sessionTimestamp + "_combined.webm";
                // Upload combined file to S3
                uploadToS3(bucketName, combinedKey, combinedData);

                // Generate S3 URL
                S3Utilities s3Utilities = s3Client.utilities();
                GetUrlRequest urlRequest = GetUrlRequest.builder()
                        .bucket(bucketName)
                        .key(combinedKey)
                        .build();
                URL s3Url = s3Utilities.getUrl(urlRequest);
                String s3UrlString = s3Url.toString();

                // Construct JSON message
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode jsonMessage = (ObjectNode) this.overallFeedback;
//                ObjectNode jsonMessage = mapper.createObjectNode();
                jsonMessage.put("type", "backend_message");
                jsonMessage.put("videoLink", s3UrlString);
                String messageStr = mapper.writeValueAsString(jsonMessage);

                System.out.println("check message");
                System.out.println(messageStr);

                // Send JSON message to client
                if (webSocketSession != null && webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(new TextMessage(messageStr));
                }

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
//            Files.delete(Paths.get(videoPath));
//            Files.delete(Paths.get(audioPath));
//            Files.delete(Paths.get(combinedPath));
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
//        if (humeAudioClient != null && humeAudioClient.isOpen()) {
//            humeAudioClient.close();
//        }

//        if (humeAIBatchProcessingClient != null) {
//            humeAIBatchProcessingClient.shutdown();
//        }
    }
}