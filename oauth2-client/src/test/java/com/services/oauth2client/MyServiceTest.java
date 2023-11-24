package com.services.oauth2client;

/**
 * @Author prakashponali
 * @Date 21/11/23
 * @Description
 */
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MyServiceTest {

    //private MyServiceWebClient myService;

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private RetryRegistry retryRegistry;
    @Mock
    private Retry retry;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/endpoint")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(retryRegistry.retry("retryService")).thenReturn(retry);

        //myService = new MyServiceWebClient(webClientBuilder, retryRegistry);
    }

    @Test
    public void testCallOtherService_Success() {
        String expectedResponse = "Success";
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(expectedResponse));

        Mono<String> result = null;// = //myService.callOtherService("");

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    // Additional test cases...
}

