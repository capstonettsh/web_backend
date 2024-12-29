package com.communication.communication_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {
    private static final String bucketName = "communication-coach-simulator-development";

    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.builder()
                .accessKeyId(accessKey)
                .secretAccessKey(secretKey)
                .build();
        return S3Client.builder()
                .credentialsProvider(() -> credentials)
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }

    public String getBucketName() {
        return bucketName;
    }
}
