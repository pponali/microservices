package com.services.authorization.controller;

import com.services.authorization.service.JpaUserDetailsManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author prakashponali
 * @Date 05/10/23
 */

@Slf4j
@RestController
@RequestMapping(value ="/api/v1")
public class SecurityController {

    @Autowired
    private JpaUserDetailsManager jpaUserDetailsManager;


    @GetMapping(value ="/index")
    public String index() {
        return "welcome to the application";
    }


    @GetMapping(value ="/secure")
    public UserDetails secure(Authentication authentication) {
        String userName = (String)authentication.getPrincipal();
        log.info("User name is {}", userName);
        return jpaUserDetailsManager.loadUserByUsername(userName);
    }




}
