package com.votasegura.api.controller;

import com.votasegura.api.dto.request.*;
import com.votasegura.api.dto.response.*;
import com.votasegura.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 3.2 Inicio de sesion
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req)));
    }

    // 3.3 Cierre de sesion
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MensajeResponse>> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LogoutRequest req) {
        String token = authHeader.substring(7);
        return ResponseEntity.ok(ApiResponse.success(authService.logout(token, req)));
    }

    // 3.6 Renovar token de acceso
    @PostMapping("/token/renovar")
    public ResponseEntity<ApiResponse<AuthResponse>> renovarToken(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.renovarToken(req)));
    }

    // 3.8 Solicitar recuperacion de password
    @PostMapping("/password/recuperar")
    public ResponseEntity<ApiResponse<MensajeResponse>> recuperarPassword(
            @Valid @RequestBody RecuperarPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.recuperarPassword(req)));
    }

    // 3.9 Restablecer password
    @PostMapping("/password/restablecer")
    public ResponseEntity<ApiResponse<MensajeResponse>> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.restablecerPassword(req)));
    }
}
