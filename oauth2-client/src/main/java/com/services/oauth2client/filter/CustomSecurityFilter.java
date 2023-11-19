package com.services.oauth2client.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */
public class CustomSecurityFilter extends OncePerRequestFilter  implements Serializable  {

    public static final String PARAMETER_LOGIN = "username";
    public static final String PARAMETER_PASSWORD = "password";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {


        System.out.println("\n \n Before filter \n \n \n");

        if(Collections.list(request.getParameterNames()).contains(PARAMETER_LOGIN) && Collections.list(request.getParameterNames()).contains(PARAMETER_PASSWORD)){
            String userName = request.getParameter(PARAMETER_LOGIN);
            String password = request.getParameter(PARAMETER_LOGIN);
            var newSecurityContext  = SecurityContextHolder.createEmptyContext();
            newSecurityContext.setAuthentication(
                    new UsernamePasswordAuthenticationToken(userName, password,
                            AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")));
            SecurityContextHolder.setContext(newSecurityContext);
        }
        filterChain.doFilter(request, response);
        System.out.println("\n \n  After filter \n \n ");
    }
}
