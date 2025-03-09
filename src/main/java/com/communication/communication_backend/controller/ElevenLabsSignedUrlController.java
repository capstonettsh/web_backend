package com.communication.communication_backend.controller;

import com.communication.communication_backend.service.ElevenLabsSignedUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ElevenLabsSignedUrlController {
    @Autowired
    private ElevenLabsSignedUrl elevenLabsSignedUrl;

    @GetMapping("/eleven-labs/signed-url")
    public ResponseEntity<SignedUrlResponse> getSignedUrl(@RequestParam("agentId") String agentId) {
        try {
            String signedUrl = elevenLabsSignedUrl.getSignedUrl(agentId);
            return ResponseEntity.ok(new SignedUrlResponse(signedUrl));
        } catch (Exception e) {
            // Log the error (consider using a logger)
            return ResponseEntity.status(500).body(new SignedUrlResponse("Error: " + e.getMessage()));
        }
    }

    // Inner class for response DTO
    public static class SignedUrlResponse {
        private String signedUrl;

        public SignedUrlResponse() {
        }

        public SignedUrlResponse(String signedUrl) {
            this.signedUrl = signedUrl;
        }

        public String getSignedUrl() {
            return signedUrl;
        }

        public void setSignedUrl(String signedUrl) {
            this.signedUrl = signedUrl;
        }
    }
}
