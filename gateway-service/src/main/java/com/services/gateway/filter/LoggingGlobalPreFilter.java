package com.services.gateway.filter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingGlobalPreFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        LocalDateTime date = LocalDateTime.now();
        MultiValueMap<String, String> map = request.getQueryParams();
        Set<String> set = map.keySet();
        for (String s : set) {
            System.err.println("Parameter: " + s + ": " + map.get(s));
        }

        log.error("------------------------------------------------------------------------" + date);
        /*System.err.println("START LOGFILTER: " + date + " - " + request.getLocalAddr() + ":" + request.getLocalPort() + request.getServletPath() + "\nRequest:");
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = (String) headers.nextElement();
            System.out.println("\tHeader: " + headerName + ":" + request.getHeader(headerName));
        }
        System.out.println("\n");
        Enumeration<String> parameters = request.getParameterNames();
        while (parameters.hasMoreElements()) {
            String parameterName = (String) parameters.nextElement();
            System.out.println("\tParameter: " + parameterName + ": " + request.getParameter(parameterName));
        }
        System.out.println("\n");
        Enumeration<String> attributes = request.getAttributeNames();
        while (attributes.hasMoreElements()) {
            String attributeName = (String) attributes.nextElement();
            System.out.println("\tAttribute: " + attributeName + ": " + request.getParameter(attributeName));
        }


        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(),
                request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(),
                response.getCharacterEncoding());

        System.out.println("Request Body: " + requestBody + "\n");
        System.out.println("Response Body: " + responseBody + "\n");
        System.out.println("\n");
        Collection<String> responseHeaders = response.getHeaderNames();
        responseHeaders.forEach(x -> System.out.println("\tHeader: " + x + ": " + response.getHeader(x)));*/
        System.out.println("\n\n");


        System.err.println("END LOG FILTER");
        log.error(date + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ \n\n");

        //responseWrapper.copyBodyToResponse();
        return chain.filter(exchange);
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
