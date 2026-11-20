package com.votasegura.api.controller;

import com.votasegura.api.dto.request.*;
import com.votasegura.api.dto.response.*;
import com.votasegura.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticación", description = "Endpoints de inicio de sesión, cierre de sesión, renovación de token y gestión de contraseñas")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario con correo y contraseña. Retorna tokens JWT de acceso y refresco.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autenticación exitosa",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Demasiados intentos. Límite de tasa excedido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req)));
    }

    @Operation(summary = "Cerrar sesión", description = "Invalida el token de acceso actual registrándolo en la lista negra.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sesión cerrada correctamente",
            content = @Content(schema = @Schema(implementation = MensajeResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token inválido o expirado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MensajeResponse>> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LogoutRequest req) {
        String token = authHeader.substring(7);
        return ResponseEntity.ok(ApiResponse.success(authService.logout(token, req)));
    }

    @Operation(summary = "Renovar token de acceso", description = "Genera un nuevo par de tokens usando un Refresh Token válido y no expirado.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens renovados correctamente",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token inválido o expirado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/token/renovar")
    public ResponseEntity<ApiResponse<AuthResponse>> renovarToken(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.renovarToken(req)));
    }

    @Operation(summary = "Solicitar recuperación de contraseña", description = "Envía un enlace de restablecimiento al correo del usuario registrado.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Correo de recuperación enviado",
            content = @Content(schema = @Schema(implementation = MensajeResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Correo no válido o no registrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/password/recuperar")
    public ResponseEntity<ApiResponse<MensajeResponse>> recuperarPassword(
            @Valid @RequestBody RecuperarPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.recuperarPassword(req)));
    }

    @Operation(summary = "Restablecer contraseña", description = "Establece una nueva contraseña usando el token de recuperación enviado por correo.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente",
            content = @Content(schema = @Schema(implementation = MensajeResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token inválido, expirado o contraseña débil",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/password/restablecer")
    public ResponseEntity<ApiResponse<MensajeResponse>> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.restablecerPassword(req)));
    }
}
