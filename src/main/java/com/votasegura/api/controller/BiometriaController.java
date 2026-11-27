package com.votasegura.api.controller;

import com.votasegura.api.dto.request.*;
import com.votasegura.api.dto.response.*;
import com.votasegura.api.service.BiometriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Biometría WebAuthn / FIDO2", description = "Registro y autenticación biométrica usando el estándar W3C WebAuthn con soporte para huella digital y Face ID")
@RestController
@RequestMapping("/api/v1/auth/biometria")
@RequiredArgsConstructor
public class BiometriaController {

    private final BiometriaService biometriaService;

    @Operation(summary = "Generar desafío de registro biométrico", description = "Genera un challenge criptográfico que el dispositivo del usuario debe firmar con su clave privada del chip de seguridad (Secure Enclave / TPM) para registrar la credencial biométrica.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Desafío de registro generado correctamente",
            content = @Content(schema = @Schema(implementation = BiometriaDesafioResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/registro/desafio")
    public ResponseEntity<ApiResponse<BiometriaDesafioResponse>> desafioRegistro(
            Authentication authentication,
            @Valid @RequestBody BiometriaDesafioRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                biometriaService.generarDesafioRegistro(authentication.getName(), req)));
    }

    @Operation(summary = "Confirmar y registrar clave pública biométrica", description = "Registra la llave pública FIDO2 del dispositivo. La llave privada nunca abandona el hardware del usuario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Credencial biométrica registrada correctamente",
            content = @Content(schema = @Schema(implementation = CredencialRegistradaResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Firma inválida o challenge expirado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/registro/confirmar")
    public ResponseEntity<ApiResponse<CredencialRegistradaResponse>> confirmarRegistro(
            Authentication authentication,
            @Valid @RequestBody BiometriaConfirmarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                biometriaService.confirmarRegistro(authentication.getName(), req)));
    }

    @Operation(summary = "Generar desafío de inicio de sesión biométrico", description = "Genera un challenge criptográfico para que el autenticador del dispositivo firme y demuestre posesión de la llave privada registrada.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Desafío de login generado correctamente",
            content = @Content(schema = @Schema(implementation = BiometriaLoginDesafioResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No se encontró credencial biométrica registrada para este usuario",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login/desafio")
    public ResponseEntity<ApiResponse<BiometriaLoginDesafioResponse>> desafioLogin(
            @Valid @RequestBody BiometriaLoginDesafioRequest req) {
        return ResponseEntity.ok(ApiResponse.success(biometriaService.generarDesafioLogin(req)));
    }

    @Operation(summary = "Validar firma biométrica y otorgar acceso", description = "Verifica la firma criptográfica del dispositivo contra la llave pública almacenada. Si es válida, retorna tokens JWT de sesión sin necesidad de contraseña.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autenticación biométrica exitosa",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Firma inválida o challenge expirado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "La firma no corresponde a la llave pública registrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login/validar")
    public ResponseEntity<ApiResponse<AuthResponse>> validarFirma(
            @Valid @RequestBody BiometriaLoginValidarRequest req) {
        return ResponseEntity.ok(ApiResponse.success(biometriaService.validarFirmaLogin(req)));
    }
}
