package com.services.oauth2client;

/**
 * @Author prakashponali
 * @Date 21/11/23
 * @Description
 */
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
public class MyServiceWebClient {
    private final WebClient webClient;
    private final Retry retry;

    public MyServiceWebClient(WebClient.Builder webClientBuilder, RetryRegistry retryRegistry) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8090").build();
        this.retry = retryRegistry.retry("retryService");
    }


    public Mono<String> callOtherService(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .transformDeferred(Retry.decorateFunction(retry, Function.identity()));

    }
}

