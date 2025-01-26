package com.communication.communication_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Value("${spring.security.oauth2.client.registration.cognito.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.cognito.client-secret}")
    private String clientSecret;

    @GetMapping("/user")
    public String getUser() {
        System.out.println("check e key" + clientId + " " + clientSecret);
        return "test for user";
    }
}
