package com.catalogservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    // @LoadBalanced is the ONLY thing that turns a plain RestTemplate into a
    // client-side-load-balanced one. The interceptor it adds rewrites
    // "http://user-service/..." into "http://<picked-instance-ip>:<port>/..."
    // by consulting Spring Cloud LoadBalancer (which pulls the instance list from Eureka).
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
