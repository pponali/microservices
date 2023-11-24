package apiclient.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		.csrf().disable()
		.cors().disable()
			.authorizeHttpRequests(authorize -> authorize
					.requestMatchers("/oauth2/authorization/myoauth2").permitAll()
					.requestMatchers("/**").permitAll()
			)
			.oauth2Login(withDefaults())
			.oauth2Client(withDefaults());
		return http.build();
	}//7411269602 venkatesh
}
