package com.services.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


/**
 * @author prakashponali
 * @Date 05/10/23
 */
@EnableWebSecurity
@SpringBootApplication
//@EnableDiscoveryClient
public class AuthorizationApplication {



    public static void main(String[] args) {
        SpringApplication.run(AuthorizationApplication.class, args);
    }
}
