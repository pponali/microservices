package com.services.resourceserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;


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
    public String protectedEndpoint(Authentication authentication, @AuthenticationPrincipal OAuth2User auth2User, Principal principal) {
        System.out.println("Authentication: " + authentication);
        System.out.println("OAuth2User: " + auth2User);
        if(principal instanceof  JwtAuthenticationToken){
            Jwt jwt = ((JwtAuthenticationToken) principal).getToken();
            System.out.println("claims " + jwt.getClaims() + "  subject " + jwt.getSubject()
                    + " token value  " + jwt.getTokenValue() + " headers " + jwt.getHeaders()
            + " audience " + jwt.getAudience() + " issuer " + jwt.getIssuer() + " expires at  " + jwt.getExpiresAt()
            + " not before "  + jwt.getNotBefore());
            System.out.println("JWT Token: " + ((JwtAuthenticationToken) principal).getToken());
        }
        System.out.println("Principal: " + principal.getName());
        return "This is a protected endpoint. Token with 'ROLE_USER' required. The current user is " + authentication.getPrincipal();
    }
}