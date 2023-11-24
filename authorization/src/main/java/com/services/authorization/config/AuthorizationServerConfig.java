package com.services.authorization.config;

import com.services.authorization.entity.Authority;
import com.services.authorization.service.JpaAuthorityService;
import com.services.authorization.service.JpaRegisteredClientRepository;
import com.services.authorization.service.JpaUserDetailsManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author prakashponali
 * @Date 05/10/23
 */


@Configuration
public class AuthorizationServerConfig {


    @Autowired
    JpaRegisteredClientRepository clientRepository;

    @Autowired
    JpaUserDetailsManager detailsManager;

    @Autowired
    JpaAuthorityService jpaAuthorityService;

    @PostConstruct
    public void loadData(){

        if(clientRepository.findByClientId("api-client") == null){
            RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("api-client")
                    .clientSecret("{noop}secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                    .redirectUri("http://auth-server:8080/login/oauth2/code/articles-client-oidc")
                    .redirectUri("http://auth-server:8080/login/oauth2/code/api-client")
                    .redirectUri("http://localhost:5555/login/oauth2/code/myoauth2")
                    .redirectUri("http://localhost:3333/login/oauth2/code/spring")
                    .redirectUri("http://127.0.0.1:8080/authorized")
                    .redirectUri("https://oauthdebugger.com/debug")
                    .redirectUri("https://oidcdebugger.com/debug")
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.ADDRESS)
                    .scope(OidcScopes.PHONE)
                    .scope(OidcScopes.EMAIL)
                    .scope(OidcScopes.OPENID)
                    .scope("read")
                    .scope("write")
                    .scope("edit")
                    .scope("articles.read")
                    .scope("articles.write")
                    .scope("articles.edit")
                    .tokenSettings(tokenSettings())
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                    .build();
            clientRepository.save(registeredClient);
        }
        if(jpaAuthorityService.authorityExists("ROLE_USER")){
            Authority authority = new Authority();
            authority.setAuthority("ROLE_USER");
            jpaAuthorityService.createAuthority(authority);
        }

        if(jpaAuthorityService.authorityExists("ROLE_ADMIN")){
            Authority admin = new Authority();
            admin.setAuthority("ROLE_ADMIN");
            jpaAuthorityService.createAuthority(admin);
        }

        if(jpaAuthorityService.authorityExists("ROLE_DEVELOPER")){
            Authority developer = new Authority();
            developer.setAuthority("ROLE_DEVELOPER");
            jpaAuthorityService.createAuthority(developer);
        }

        if(!detailsManager.userExists("user")){
            UserDetails user = User.withDefaultPasswordEncoder()
                    .username("user")
                    .password("password")
                    .roles("USER", "ADMIN")
                    .accountExpired(Boolean.TRUE)
                    .accountLocked(Boolean.TRUE)
                    .credentialsExpired(Boolean.TRUE)
                    .authorities("ROLE_USER")
                    .build();
            detailsManager.createUser(user);
        }

        if(!detailsManager.userExists("admin")){
            UserDetails user = User.withDefaultPasswordEncoder()
                    .username("admin")
                    .password("password")
                    .accountExpired(Boolean.TRUE)
                    .accountLocked(Boolean.TRUE)
                    .credentialsExpired(Boolean.TRUE)
                    .authorities( "ROLE_ADMIN")
                    .build();
            detailsManager.createUser(user);
        }

        if(!detailsManager.userExists("developer")){
            UserDetails user = User.withDefaultPasswordEncoder()
                    .username("developer")
                    .password("password")
                    .accountExpired(Boolean.TRUE)
                    .accountLocked(Boolean.TRUE)
                    .credentialsExpired(Boolean.TRUE)
                    .authorities( "ROLE_DEVELOPER")
                    .build();
            detailsManager.createUser(user);
        }

    }



    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://auth-server:8080")
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .jwkSetEndpoint("/oauth2/jwks")
                .oidcUserInfoEndpoint("/userinfo")
                //.oidcClientRegistrationEndpoint("/clientregistration")
                .build();
    }




    @Bean
    public TokenSettings tokenSettings() {
        return TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .refreshTokenTimeToLive(Duration.ofDays(180))
                .accessTokenTimeToLive(Duration.ofMinutes(180))
                .build();
    }


    @Bean
    public ClientSettings clientSettings() {
        return ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false)
                .build();
    }


    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(OidcUserInfoService oidcUserInfoService) {
        return context -> {
            Authentication principle = context.getPrincipal();
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                //oidcUserInfoService.addClaims(context);
                context.getClaims().claim("test", "Test Id token");
            }
            if (OAuth2TokenType.ACCESS_TOKEN.toString().equals(context.getTokenType().getValue())) {
                context.getClaims().claim("test", "Test access token");
                Set<String> authorities = principle.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
                context.getClaims().claim("authorities", authorities)
                        .claim("user", principle.getName());
            }
        };
    }
}







