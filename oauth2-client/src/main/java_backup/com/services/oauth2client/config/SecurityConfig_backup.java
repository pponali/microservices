package com.services.oauth2client.config;

import com.services.oauth2client.filter.LoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @Author prakashponali
 * @Date 18/11/23
 * @Description
 */
@EnableWebSecurity
@Slf4j
public class SecurityConfig_backup  {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        /*http
                .csrf().disable()
                .cors().disable()
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()) //.authenticated())
                .oauth2Login(withDefaults())
                .oauth2Client(withDefaults());
        http.addFilterBefore(new LoggingFilter(), HeaderWriterFilter.class);*/
        return http.build();
    }
    /*SecurityFilterChain configureFilterChain(HttpSecurity http) throws  Exception{
        http.authorizeHttpRequests(authconfig -> authconfig.anyRequest().authenticated())
                .oauth2Login(oath2 -> oath2.loginPage("/oauth2/authorization/myoauth2"))
                .oauth2Client(Customizer.withDefaults());
        http.addFilterBefore(new LoggingFilter(), HeaderWriterFilter.class);

        return http.build();
    }*/
/*
    @Bean
    @Order(100)
    SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
        http   .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .headers(Customizer.withDefaults())
                .addFilterBefore(new CustomSecurityFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authconfig -> authconfig.requestMatchers(HttpMethod.GET, "/").permitAll())
                .authorizeHttpRequests(authconfig -> authconfig.requestMatchers(HttpMethod.GET, "/user").hasAnyRole("ROLE_USER", "OIDC_USER"))
                .authorizeHttpRequests(authconfig -> authconfig.requestMatchers(HttpMethod.GET, "/admin").hasAnyRole("ROLE_ADMIN"))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return http.build();

    }

    @Bean
    @Order(101)
    SecurityFilterChain configureSecurityFilterForAdmin(HttpSecurity http) throws Exception {
        http.securityMatcher("/admin/**")
                .authorizeHttpRequests(authconfig -> {
                    authconfig.requestMatchers("/admin/**").hasAnyRole("ROLE_USER", "ROLE_ADMIN");
                    authconfig.anyRequest().authenticated();
                })
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(102)
    SecurityFilterChain configureSecurityFilterForUser(HttpSecurity http) throws Exception{
        http.securityMatcher("/user/**")
                .authorizeHttpRequests(authconfig -> {
                    authconfig.requestMatchers("/user/**").hasRole("ROLE_USER");
                    authconfig.anyRequest().authenticated();
                })
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(103)
    SecurityFilterChain configureSecurityFilterForPublic(HttpSecurity http) throws Exception{
        http.securityMatcher("/**")
                .authorizeHttpRequests(authconfig -> authconfig.anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    JdbcUserDetailsManager  jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    DataSource getDatasource(){
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
                .username("sa")
                .password("")
                .build();
    }



    @Bean
    UserDetailsService  userDetailsService() {

        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                if(username.equals("user")){
                    return User.builder()
                            .authorities("ROLE_USER")
                            .roles("USER")
                            .username(username)
                            .password("password").build();
                } else {
                    throw new UsernameNotFoundException("User not found with username: " + username);
                }
            }
        };


    }

    @Bean
    PasswordEncoder  passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }*/


    @Bean
    ApplicationListener<AuthenticationSuccessEvent> authenticationSuccessListener() {
        return event -> {
            log.error("Success long in " + event.getAuthentication().getClass().getName() + " - " + event.getAuthentication().getName());
        };
    }

    @Bean
    ApplicationListener<AuthenticationFailureBadCredentialsEvent> authenticationFailureBadCredentialsListener() {
        return event -> {
            log.error("Failure long in " + event.getAuthentication().getClass().getName() + " - " + event.getAuthentication().getName());
        };
    }

    @Bean
    ApplicationListener<AuthenticationFailureExpiredEvent> authenticationFailureExpiredEventListener() {
        return event -> {
            log.error("Failure long in " + event.getAuthentication().getClass().getName() + " - " + event.getAuthentication().getName());
        };
    }




}
