package com.ccbsa.wms.user.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI configuration. Configures API documentation for the user-service.
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI().info(new Info().title("User Service API")
                        .description("User Management Service API - Provides user management operations, IAM integration, and BFF authentication endpoints")
                        .version("1.0.0")
                        .contact(new Contact().name("WMS Development Team")
                                .email("dev@ccbsa.com"))
                        .license(new License().name("Proprietary")
                                .url("https://ccbsa.com")))
                .servers(List.of(new Server().url("http://localhost:8088")
                        .description("Development Server"), new Server().url("https://api.ccbsa.com")
                        .description("Production Server")));
    }
}

