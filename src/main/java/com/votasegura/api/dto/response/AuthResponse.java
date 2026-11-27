package com.votasegura.api.dto.response;

import com.votasegura.api.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de autenticación con tokens JWT e información del usuario")
public class AuthResponse {

    @Schema(description = "Token de acceso JWT (Bearer)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Token de refresco para renovar el acceso sin re-autenticarse", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Tipo de token, siempre 'Bearer'", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Tiempo de expiración del access token en segundos", example = "900")
    private long expiresIn;

    @Schema(description = "Información básica del usuario autenticado")
    private UsuarioInfo usuario;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Información resumida del usuario autenticado")
    public static class UsuarioInfo {

        @Schema(description = "Identificador único del usuario (UUID)", example = "usr-3d8f1a2b-4c7e")
        private String id;

        @Schema(description = "Nombre completo del usuario", example = "Ana García López")
        private String nombre;

        @Schema(description = "Correo electrónico del usuario", example = "ana.garcia@correo.com")
        private String correo;

        @Schema(description = "Rol del usuario en el sistema", example = "VOTANTE", allowableValues = {"VOTANTE", "ADMIN"})
        private Rol rol;
    }
}
