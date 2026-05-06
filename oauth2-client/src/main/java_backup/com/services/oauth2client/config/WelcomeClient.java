package com.services.oauth2client.config;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@HttpExchange("http://localhost:8090")
public interface WelcomeClient {

    @GetExchange("/")
    String getWelcome();
}
