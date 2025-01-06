package com.communication.communication_backend.controller;

//import com.example.demo.model.TokenResponse;
//import com.example.demo.service.CognitoAuthService;

import com.communication.communication_backend.dtos.CognitoTokenResponseDto;
import com.communication.communication_backend.service.CognitoAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private CognitoAuthService cognitoAuthService;

    @PostMapping("/token")
    public ResponseEntity<CognitoTokenResponseDto> exchangeCodeForTokens(@RequestParam String code) {
        System.out.println("code here nice");
        return cognitoAuthService.exchangeCodeForTokens(code);
    }
}