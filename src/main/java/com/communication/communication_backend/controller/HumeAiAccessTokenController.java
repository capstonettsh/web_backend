package com.communication.communication_backend.controller;

import com.communication.communication_backend.service.HumeAiAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class HumeAiAccessTokenController {
    @Autowired
    private HumeAiAccessToken humeAiAccessToken;

    @PostMapping("/access-token")
    public ResponseEntity<AccessTokenResponse> getAccessToken() {
        try {
            String accessToken = humeAiAccessToken.getAccessToken();
            return ResponseEntity.ok(new AccessTokenResponse(accessToken));
        } catch (Exception e) {
            // Log the error (consider using a logger)
            return ResponseEntity.status(500).body(new AccessTokenResponse("Error: " + e.getMessage()));
        }
    }

    // Inner class for response DTO
    public static class AccessTokenResponse {
        private String accessToken;

        public AccessTokenResponse() {
        }

        public AccessTokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
