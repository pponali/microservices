package com.services.resourceserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author prakashponali
 * @Date 24/11/23
 * @Description
 */

@RestController
public class HomeController {

    @GetMapping("/")
    public String publicEndpoint() {
        return "This is a public endpoint. No token required.";
    }
}
