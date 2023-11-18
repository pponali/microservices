package com.services.authorization.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author prakashponali
 * @Date 05/10/23
 */

@RestController
@RequestMapping(value ="/api/v1")
public class SecurityController {


    @GetMapping(value ="/index")
    public String index() {
        return "welcome to the application";
    }


    @GetMapping(value ="/secure")
    public String secure() {
        return "This is secure page only authenticated users can access";
    }




}
