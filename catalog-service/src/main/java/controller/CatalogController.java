package com.catalogservice.controller;

import com.catalogservice.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping
    public String getCatalog() {
        return "Hello from catalog service";
    }

    // Client-side LB via OpenFeign (the modern industry default).
    @GetMapping("/with-user-feign")
    public Map<String, Object> viaFeign() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("calledVia", "OpenFeign + Spring Cloud LoadBalancer");
        response.put("user", userFeignClient.whoami());
        return response;
    }

    // Client-side LB via @LoadBalanced RestTemplate (the classic mechanism).
    // Note the URL uses the Eureka service name, not a host:port.
    @SuppressWarnings("unchecked")
    @GetMapping("/with-user-rest")
    public Map<String, Object> viaRestTemplate() {
        Map<String, Object> user = restTemplate.getForObject(
                "http://user-service/users/whoami", Map.class);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("calledVia", "@LoadBalanced RestTemplate + Spring Cloud LoadBalancer");
        response.put("user", user);
        return response;
    }
}
