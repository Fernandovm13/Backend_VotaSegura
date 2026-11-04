package com.votasegura.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API REST VotaSegura",
        version = "1.0.0",
        description = "Documentación interactiva de endpoints RESTful para la plataforma de votaciones VotaSegura",
        contact = @Contact(name = "Equipo VotaSegura", email = "soporte@votasegura.com")
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Ingrese el token JWT obtenido en el endpoint /api/v1/auth/login"
)
public class OpenApiConfig {
}
