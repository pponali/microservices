package com.services.resourceserver.config;

import com.services.resourceserver.filter.LogFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * OAuth2 resource server security configuration.
 *
 * <p>The {@link JwtDecoder} is exposed as a separate bean so that {@code @WebMvcTest} slices
 * can override it with {@code @MockBean JwtDecoder} without triggering an issuer OIDC
 * metadata fetch during test context startup.</p>
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(rsc -> rsc.jwt(Customizer.withDefaults()));
        http.addFilterBefore(new LogFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
