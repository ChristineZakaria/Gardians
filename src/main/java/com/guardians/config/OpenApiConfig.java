package com.guardians.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Guardians API",
        version = "1.0.0",
        description = "Parental Control App Backend — Auth, Pairing & Alerts",
        contact = @Contact(name = "Guardians Team")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Paste your JWT token here (without 'Bearer ' prefix)"
)
public class OpenApiConfig {}
