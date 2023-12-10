package com.services.common.controller;

import com.services.common.service.AsyncService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @Author prakashponali
 * @Date 21/11/23
 * @Description
 */

@Slf4j
@RestController
public class WelcomeController {

    private final AsyncService asyncService;
    @SneakyThrows
    public WelcomeController(AsyncService asyncService) {
        this.asyncService = asyncService;
    }

    @GetMapping("/welcome")
    public String welcome() {
        String orderId = "order1";
        asyncService.asyncMethodWithVoidReturnType();
        asyncService.asyncMethodWithReturnType();
        asyncService.asyncMethodWithParameter(orderId);
        asyncService.asyncMethodWithParameterAndReturnType(orderId);
        try {
            asyncService.asyncMethodWithException(orderId);
        } catch (Exception e) {
            log.error("exception in asyncMethodWithException"  + Thread.currentThread());
        }
        try {
            asyncService.asyncMethodWithVoidReturnTypeAndException(orderId);
        } catch (Exception e) {
            log.error("exception in asyncMethodWithVoidReturnTypeAndException" + Thread.currentThread());
        }
        try {
            asyncService.asyncMethodWithExceptionAndReturnType(orderId);
        } catch (Exception e) {
            log.error("exception in asyncMethodWithExceptionAndReturnType" + Thread.currentThread());
        }
        try {
            asyncService.asyncMethodWithVoidReturnTypeAndException(orderId);
        } catch (Exception e) {
            log.error("exception in asyncMethodWithVoidReturnTypeAndException" + Thread.currentThread());
        }
        return "Welcome to the application" + LocalDateTime.now();

    }
}
