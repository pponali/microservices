package com.services.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @Author prakashponali
 * @Date 21/11/23
 * @Description
 */

@RestController
public class WelecomeController {
    @GetMapping("/welcome")
    public String welcome(){
        return "Welcome to the application" + LocalDateTime.now();
    }
}
