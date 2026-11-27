package com.votasegura.api.controller;

import com.votasegura.api.dto.request.OtpSolicitarRequest;
import com.votasegura.api.dto.request.OtpVerificarRequest;
import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.OtpResponse;
import com.votasegura.api.dto.response.OtpVerificadoResponse;
import com.votasegura.api.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OTP - Verificación", description = "Solicitud y verificación de códigos de un solo uso (OTP) para verificación de cuenta, 2FA y recuperación de contraseña")
@RestController
@RequestMapping("/api/v1/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @Operation(summary = "Solicitar código OTP", description = "Genera y envía un código OTP de 6 dígitos al correo o teléfono del usuario según el canal y propósito indicados.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP generado y enviado correctamente",
            content = @Content(schema = @Schema(implementation = OtpResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Canal o propósito inválido / correo no registrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Demasiadas solicitudes de OTP. Límite de tasa excedido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/solicitar")
    public ResponseEntity<ApiResponse<OtpResponse>> solicitar(@Valid @RequestBody OtpSolicitarRequest req) {
        return ResponseEntity.ok(ApiResponse.success(otpService.solicitar(req)));
    }

    @Operation(summary = "Verificar código OTP", description = "Valida el código OTP recibido por el usuario. Si es correcto, activa la cuenta o permite continuar el flujo según el propósito.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verificado correctamente",
            content = @Content(schema = @Schema(implementation = OtpVerificadoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Código OTP inválido, expirado o ya utilizado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/verificar")
    public ResponseEntity<ApiResponse<OtpVerificadoResponse>> verificar(@Valid @RequestBody OtpVerificarRequest req) {
        return ResponseEntity.ok(ApiResponse.success(otpService.verificar(req)));
    }
}
