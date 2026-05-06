package com.services.authorization.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Enumeration;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LocalDateTime date = LocalDateTime.now();
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        log.error("------------------------------------------------------------------------");
        log.error("START LOGFILTER: " + date + " - " + request.getLocalAddr() + ":" + request.getLocalPort() + request.getServletPath() + "\nRequest:");
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = (String) headers.nextElement();
            log.error("\tHeader: " + headerName + ":" + request.getHeader(headerName));
        }
        log.error("\n");
        Enumeration<String> parameters = request.getParameterNames();
        while (parameters.hasMoreElements()) {
            String parameterName = (String) parameters.nextElement();
            log.error("\tParameter: " + parameterName + ": " + request.getParameter(parameterName));
        }
        log.error("\n");
        Enumeration<String> attributes = request.getAttributeNames();
        while (attributes.hasMoreElements()) {
            String attributeName = (String) attributes.nextElement();
            log.error("\tAttribute: " + attributeName + ": " + request.getParameter(attributeName));
        }

        filterChain.doFilter(requestWrapper, responseWrapper);

        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(),
                request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(),
                response.getCharacterEncoding());

        log.error("Request Body: " + requestBody + "\n");
        log.error("Response Body: " + responseBody + "\n");
        log.error("\n");
        Collection<String> responseHeaders = response.getHeaderNames();
        responseHeaders.forEach(x -> log.error("\tHeader: " + x + ": " + response.getHeader(x)));
        log.error("\n\n");


        System.err.println("END LOG FILTER");
        log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");

        responseWrapper.copyBodyToResponse();
    }

    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            return new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
