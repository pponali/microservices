package com.services.authorization.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.services.authorization.filter.LoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.oidc.web.OidcUserInfoEndpointFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.function.Function;

/**
 * @author prakashponali
 * @Date 05/10/23
 */


@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    @Order(100)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.addFilterBefore(new LoggingFilter(), UsernamePasswordAuthenticationFilter.class);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)//.oidc(Customizer.withDefaults());
         .oidc((oidc) -> oidc
                .userInfoEndpoint((userInfo) -> {
                            userInfo.userInfoMapper(userInfoMapper());
                        }
                        ));
        //.oidc(oidcConfigurer -> {oidcConfigurer.userInfoEndpoint(userInfo -> {
        //    userInfo.userInfoMapper(userInfoMapper());
       // });});
        //AuthenticationManager = http.getSharedObject(AuthenticationManager.class);
        //http.addFilterAfter(new OidcUserInfoEndpointFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        http.oauth2ResourceServer(resourceServerConfigurer -> {
            resourceServerConfigurer.jwt(Customizer.withDefaults());
        });
        return http.formLogin(Customizer.withDefaults()).build();
    }

    @Order(101)
    public SecurityFilterChain authServerSecurityFilterChainJwt(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(resourceServerConfigurer -> {
            resourceServerConfigurer.jwt(Customizer.withDefaults());
        });

        return http.build();
    }

    private Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper() {
        return context -> {
            OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
            JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();
            return new OidcUserInfo(principal.getToken().getClaims());
        };
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    private static RSAKey generateRsa() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();


        return new RSAKey.Builder(publicKey).privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("bael-key-id")
                .build();
    }

    private static KeyPair generateRsaKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }


    @Bean
    @Order(101)
    SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) .cors(AbstractHttpConfigurer::disable)
            //.authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/.well-known/openid-configuration").permitAll())
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers(HttpMethod.GET, "/userinfo").authenticated())
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/logout").permitAll())
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers(HttpMethod.POST, "/login").permitAll())
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/oauth2/authorization/**").permitAll())
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/oauth2/token/**").permitAll())
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/**").permitAll())
               // .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/oauth2/introspect").permitAll())
               // .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/userinfo").permitAll())
            .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
