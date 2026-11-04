package com.votasegura.api.controller;

import com.votasegura.api.dto.request.OtpSolicitarRequest;
import com.votasegura.api.dto.request.OtpVerificarRequest;
import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.OtpResponse;
import com.votasegura.api.dto.response.OtpVerificadoResponse;
import com.votasegura.api.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    // 3.4 Solicitar OTP
    @PostMapping("/solicitar")
    public ResponseEntity<ApiResponse<OtpResponse>> solicitar(@Valid @RequestBody OtpSolicitarRequest req) {
        return ResponseEntity.ok(ApiResponse.success(otpService.solicitar(req)));
    }

    // 3.5 Verificar OTP
    @PostMapping("/verificar")
    public ResponseEntity<ApiResponse<OtpVerificadoResponse>> verificar(@Valid @RequestBody OtpVerificarRequest req) {
        return ResponseEntity.ok(ApiResponse.success(otpService.verificar(req)));
    }
}
