package com.votasegura.api.controller;

import com.votasegura.api.dto.request.*;
import com.votasegura.api.dto.response.*;
import com.votasegura.api.service.BiometriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/biometria")
@RequiredArgsConstructor
public class BiometriaController {

    private final BiometriaService biometriaService;

    // 3.10 Desafio para registro biometrico (protegido)
    @PostMapping("/registro/desafio")
    public ResponseEntity<ApiResponse<BiometriaDesafioResponse>> desafioRegistro(
            Authentication authentication,
            @Valid @RequestBody BiometriaDesafioRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                biometriaService.generarDesafioRegistro(authentication.getName(), req)));
    }

    // 3.11 Confirmar y registrar clave publica biometrica (protegido)
    @PostMapping("/registro/confirmar")
    public ResponseEntity<ApiResponse<CredencialRegistradaResponse>> confirmarRegistro(
            Authentication authentication,
            @Valid @RequestBody BiometriaConfirmarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                biometriaService.confirmarRegistro(authentication.getName(), req)));
    }

    // 3.12 Desafio para login biometrico (publico)
    @PostMapping("/login/desafio")
    public ResponseEntity<ApiResponse<BiometriaLoginDesafioResponse>> desafioLogin(
            @Valid @RequestBody BiometriaLoginDesafioRequest req) {
        return ResponseEntity.ok(ApiResponse.success(biometriaService.generarDesafioLogin(req)));
    }

    // 3.13 Validar firma biometrica y otorgar acceso (publico)
    @PostMapping("/login/validar")
    public ResponseEntity<ApiResponse<AuthResponse>> validarFirma(
            @Valid @RequestBody BiometriaLoginValidarRequest req) {
        return ResponseEntity.ok(ApiResponse.success(biometriaService.validarFirmaLogin(req)));
    }
}
