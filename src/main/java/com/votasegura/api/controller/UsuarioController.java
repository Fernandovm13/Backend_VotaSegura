package com.votasegura.api.controller;

import com.votasegura.api.dto.request.ActualizarPerfilRequest;
import com.votasegura.api.dto.request.RegistroRequest;
import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.HistorialVotoResponse;
import com.votasegura.api.dto.response.PerfilResponse;
import com.votasegura.api.dto.response.UsuarioResponse;
import com.votasegura.api.service.AuthService;
import com.votasegura.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    // 3.1 Registro de nuevo usuario
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> registrar(@Valid @RequestBody RegistroRequest req) {
        UsuarioResponse usuario = authService.registrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(usuario));
    }

    // 3.7 Consultar perfil del usuario autenticado
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<PerfilResponse>> perfil(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.obtenerPerfil(authentication.getName())));
    }

    // Actualizar datos del perfil del usuario autenticado
    @PutMapping("/perfil")
    public ResponseEntity<ApiResponse<PerfilResponse>> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest req,
            Authentication authentication) {
        PerfilResponse response = usuarioService.actualizarPerfil(authentication.getName(), req);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Historial de votos del usuario autenticado (datos reales de BD)
    @GetMapping("/historial-votos")
    public ResponseEntity<ApiResponse<List<HistorialVotoResponse>>> historialVotos(Authentication authentication) {
        List<HistorialVotoResponse> historial = usuarioService.obtenerHistorialVotos(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(historial));
    }

    // Desactivar cuenta del usuario autenticado
    @DeleteMapping("/perfil")
    public ResponseEntity<ApiResponse<String>> desactivarCuenta(Authentication authentication) {
        usuarioService.desactivarCuenta(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cuenta desactivada correctamente."));
    }
}
