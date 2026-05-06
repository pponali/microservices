package com.catalogservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

// "user-service" is the Eureka service name (NOT a hostname).
// Feign + Spring Cloud LoadBalancer pick a healthy instance for every call,
// so calling userFeignClient.whoami() is the client-side load balancing demo.
@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/users/whoami")
    Map<String, Object> whoami();
}
