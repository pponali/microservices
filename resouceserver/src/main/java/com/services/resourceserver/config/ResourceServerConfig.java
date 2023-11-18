package com.services.resourceserver.config;

import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * @author prakashponali
 * @Date 05/10/23
 */
@EnableWebSecurity
public class ResourceServerConfig {

    private final OAuth2AuthorizationServerProperties authorizationServerProperties;

    public ResourceServerConfig(OAuth2AuthorizationServerProperties authorizationServerProperties) {
        this.authorizationServerProperties = authorizationServerProperties;
    }

    @Bean
    ResourceServerConfig configureResourceServer() {
        return new ResourceServerConfig(authorizationServerProperties);
    }



}