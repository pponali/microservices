package com.services.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-registers a uniform {@link OpenAPI} bean for every servlet service that depends on common-service.
 *
 * <p>Each service shows up at:
 * <ul>
 *   <li>{@code /v3/api-docs} — JSON</li>
 *   <li>{@code /swagger-ui.html} — interactive UI</li>
 * </ul>
 * The doc title is taken from {@code spring.application.name} so Eureka and OpenAPI stay aligned.</p>
 *
 * <p>Each service is free to override the bean by declaring its own {@code @Bean OpenAPI customApi()}.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
public class CommonOpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI commonOpenApi(
            @Value("${spring.application.name:service}") String appName,
            @Value("${info.app.description:Microservices API}") String description,
            @Value("${info.app.version:0.0.1-SNAPSHOT}") String version,
            @Value("${info.app.gateway-url:http://localhost:8080}") String gatewayUrl) {

        // Bearer-JWT scheme so Swagger UI offers an "Authorize" button.
        // Services not protected by JWT can still expose this — it's harmless if unused.
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("%s API".formatted(appName))
                        .description(description)
                        .version(version)
                        .contact(new Contact().name("Platform Team").email("platform@services.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url(gatewayUrl).description("Gateway"),
                        new Server().url("/").description("Direct (this service)")
                ))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
