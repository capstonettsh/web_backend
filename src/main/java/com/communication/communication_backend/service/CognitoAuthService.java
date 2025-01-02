package com.communication.communication_backend.service;

import com.communication.communication_backend.dtos.CognitoTokenResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class CognitoAuthService {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    @Value("${spring.security.oauth2.client.registration.cognito.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.cognito.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.cognito.redirect-uri}")
    private String redirectUri;
    @Value("${spring.security.oauth2.client.provider.cognito.token-uri}")
    private String tokenUri;

    public ResponseEntity<CognitoTokenResponseDto> exchangeCodeForTokens(String code) {
        String urlStr = "https://ap-southeast-1kxlluhw1w.auth.ap-southeast-1.amazoncognito.com" + "/oauth2/token?"
                + "grant_type=authorization_code" +
                "&client_id=" + clientId +
                "&code=" + code +
                "&redirect_uri=http://localhost:3000/callback";

        String authenticationInfo = clientId + ":" + clientSecret;
        String basicAuthenticationInfo = Base64.getEncoder().encodeToString(authenticationInfo.getBytes());

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(new URI(urlStr))
                    .header("Content-type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basicAuthenticationInfo)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to build Cognito URL");
        }

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Unable to request Cognito");
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Authentication failed");
        }

        CognitoTokenResponseDto token;
        try {
            token = JSON_MAPPER.readValue(response.body(), CognitoTokenResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to decode Cognito response");
        }

        return ResponseEntity.ok(token);
    }
}
