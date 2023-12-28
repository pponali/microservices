package com.services.common.controller;

import org.springframework.ai.openai.client.OpenAiChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author prakashponali (@pponali)
 * @Date 21/12/23 4:22 pm
 * @Description :
 */
@RestController
public class OpenAIController {

    private final OpenAiChatClient aiClient;

    public OpenAIController(OpenAiChatClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/ai/simple")
    public String completion(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return aiClient.generate(message);
    }
}
