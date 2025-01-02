package com.communication.communication_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/user")
    public String getUser() {
        return "test for user";
    }
}
