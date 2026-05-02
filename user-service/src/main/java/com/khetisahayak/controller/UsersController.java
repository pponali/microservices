package com.khetisahayak.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UsersController {
    
    @GetMapping
    public String getUsers() {
        return "Hello from user service";
    }
}