package com.services.resourceserver.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        LocalDateTime date = LocalDateTime.now();

        log.trace("Request local date " + date + " local address "
                + request.getLocalAddr() + " port " + request.getLocalPort() + " Path " + request.getServletPath());
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            log.trace(header + " " + request.getHeader(header));
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

}
