package com.services.oauth2client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author prakashponali
 * @Date 18/11/23
 * @Description
 */

@RestController
public class HomePageController {

    @GetMapping("/")
    public String home() {
        return "home";
    }
}
