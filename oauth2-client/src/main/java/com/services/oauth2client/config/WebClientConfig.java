package com.services.oauth2client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */
public class WebClientConfig {

    @Bean
    WelcomeClient welcomeClient(OAuth2AuthorizedClientManager  authorizedClientManager) {
        return httpProxyFactory(authorizedClientManager).createClient(WelcomeClient.class);
    }

    @Bean
    HttpServiceProxyFactory httpProxyFactory(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction filterFunction =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        filterFunction.setDefaultOAuth2AuthorizedClient(true);
        WebClient webClient = WebClient.builder().apply(filterFunction.oauth2Configuration()).build();
       // WebClientAdapter webClientAdapter = WebClientAdapter.create(webClient);
        return HttpServiceProxyFactory.builder().build();
    }
    @Bean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
                                             OAuth2AuthorizedClientRepository repository){

        OAuth2AuthorizedClientProvider  provider = OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().refreshToken().build();

        DefaultOAuth2AuthorizedClientManager  manager = new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, repository);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }


}
