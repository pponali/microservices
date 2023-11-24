package willydekeyser.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import willydekeyser.entity.Authority;
import willydekeyser.service.JpaAuthorityService;

@Configuration
public class SecurityConfig {


	@Autowired
	JpaAuthorityService jpaAuthorityService;


	@Autowired
	JpaUserDetailsManager detailsManager;
	@PostConstruct
	public void loadData(){


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
	@Order(1)
	SecurityFilterChain asSecurityFilterChain(HttpSecurity http) throws Exception {
		
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		return http
				.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
				.oidc(withDefaults())
				.and()
				.exceptionHandling(e -> e
				.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
				.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(authorize ->authorize.anyRequest().authenticated())
				.formLogin(withDefaults())
				.build();
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
				.oidcClientRegistrationEndpoint("/connect/register")
				.build();
	}
	
	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(OidcUserInfoService userInfoService) {
		return context -> {
			Authentication principal = context.getPrincipal();
			if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
				context.getClaims().claim("Test", "Test Id Token");
				OidcUserInfo userInfo = userInfoService.loadUser( 
						context.getPrincipal().getName());
				context.getClaims().claims(claims ->
						claims.putAll(userInfo.getClaims()));
			}
			if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				context.getClaims().claim("Test", "Test Access Token");
				Set<String> authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
                context.getClaims().claim("authorities", authorities)
                        .claim("user", principal.getName());
			}
		};
	}
	
	@Bean 
	JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}
	
	@Bean
	JWKSource<SecurityContext> jwkSource() {
		RSAKey rsaKey = generateRsa();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

	public static RSAKey generateRsa() {
		KeyPair keyPair = generateRsaKey();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
	}

	static KeyPair generateRsaKey() {
		KeyPair keyPair;
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			keyPair = keyPairGenerator.generateKeyPair();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		return keyPair;
	}
}
