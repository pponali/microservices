package com.services.resourceserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author prakashponali
 * @Date 05/10/23
 */

@RequestMapping (value ="/articles")
@RestController
public class ArticlesController {


    @PreAuthorize("hasPermission('read')")
    @GetMapping("/all")
    public String[] getArticles() {
        return new String[] { "Article 1", "Article 2", "Article 3" };
    }

    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is a public endpoint. No token required.";
    }

    @GetMapping("/protected")
    @PreAuthorize("hasRole('ROLE_USER') and hasPermission('read')")
    public String protectedEndpoint() {
        return "This is a protected endpoint. Token with 'ROLE_USER' required.";
    }
}