package com.services.common.config;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.autoconfigure.openai.OpenAiProperties;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.client.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author prakashponali (@pponali)
 * @Date 21/12/23 5:57 pm
 * @Description :
 */

@Configuration
public class AiConfig {


    @Value("${spring.ai.openai.api-key}")
    private String apiKey;



    /*@Bean
    OpenAiChatClient openAiChatClient(){
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey(apiKey);
        return openAiAutoConfiguration.openAiChatClient(openAiApi(), openAiProperties);
    }

    @Bean
    OpenAiApi  openAiApi(){
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey(apiKey);
        return openAiAutoConfiguration.openAiApi(openAiProperties);
    }*/


}
