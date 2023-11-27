package com.services.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @Author prakashponali
 * @Date 27/11/23
 * @Description
 */

@Configuration
public class AsyncConfiguration {

    @Bean (name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setCorePoolSize(10);
        executor.setQueueCapacity(150);
        return executor;
    }
}
