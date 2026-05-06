package com.khetisahayak.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UsersController {

    @Value("${server.port}")
    private int port;

    @GetMapping
    public String getUsers() {
        return "Hello from user service on port " + port;
    }

    @GetMapping("/whoami")
    public Map<String, Object> whoami() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "user-service");
        info.put("port", port);
        try {
            info.put("host", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            info.put("host", "unknown");
        }
        return info;
    }
}
