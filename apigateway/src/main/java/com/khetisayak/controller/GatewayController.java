package com.khetisayak.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {
    
    @GetMapping("/test")
    public String getGateway() {
        return "Hello from gateway";
    }
}