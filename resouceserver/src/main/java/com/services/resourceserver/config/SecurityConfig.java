package com.services.resourceserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author prakashponali
 * @Date 07/10/23
 */

@EnableWebSecurity
@Configuration
public class SecurityConfig {


    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.anyRequest().authenticated();
                }).oauth2ResourceServer(resourceServerConfigurer -> {
                    resourceServerConfigurer
                            .jwt(jwtConfigurer -> {jwtConfigurer.decoder(JwtDecoders.fromIssuerLocation(issuerUri));
                    });
                });

        return http.build();

    }
}
