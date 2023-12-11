package com.services.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Author prakashponali
 * @Date 27/11/23
 * @Description
 */

@Slf4j
@Service
public class AsyncService {

    public void asyncMethodWithVoidReturnType() {
        log.info("Inside asyncMethodWithVoidReturnType" + Thread.currentThread());
    }


    public String asyncMethodWithReturnType() {
        log.info("Inside asyncMethodWithReturnType" + Thread.currentThread());
        return "Hello World";
    }

    @Async(value = "asyncExecutor")
    public void asyncMethodWithParameter(String parameter) {
        log.info("Inside asyncMethodWithParameter: " + parameter + Thread.currentThread());
    }

    @Async(value = "asyncExecutor")
    public void asyncMethodWithParameterAndReturnType(String parameter) {
        log.info("Inside asyncMethodWithParameterAndReturnType: " + parameter + Thread.currentThread());
    }

    @Async(value = "asyncExecutor")
    public void asyncMethodWithException(String parameter) throws Exception {
        log.info("Inside asyncMethodWithException: " + parameter);
        throw new Exception("Exception occurred");
    }

    @Async(value = "asyncExecutor")
    public void asyncMethodWithExceptionAndReturnType(String parameter) throws Exception {
        log.info("Inside asyncMethodWithExceptionAndReturnType: " + parameter);
        throw new Exception("Exception occurred");
    }

    @Async(value = "asyncExecutor")
    public void asyncMethodWithVoidReturnTypeAndException(String parameter) throws Exception {
        log.info("Inside asyncMethodWithVoidReturnTypeAndException: " + parameter);
        throw new Exception("Exception occurred");
    }
}
