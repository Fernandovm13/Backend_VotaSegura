package com.votasegura.api.controller;

import com.votasegura.api.dto.request.ActualizarPerfilRequest;
import com.votasegura.api.dto.request.RegistroRequest;
import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.HistorialVotoResponse;
import com.votasegura.api.dto.response.PerfilResponse;
import com.votasegura.api.dto.response.UsuarioResponse;
import com.votasegura.api.service.AuthService;
import com.votasegura.api.service.UsuarioService;
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

import java.util.List;

@Tag(name = "Usuarios", description = "Registro de cuenta, consulta y actualización de perfil, historial de votos y desactivación de cuenta")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Registrar nuevo usuario", description = "Crea una cuenta nueva en el sistema. El correo queda en estado pendiente hasta verificar el OTP.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuario registrado correctamente",
            content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (formato de correo, contraseña débil, etc.)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El correo ya se encuentra registrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> registrar(@Valid @RequestBody RegistroRequest req) {
        UsuarioResponse usuario = authService.registrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(usuario));
    }

    @Operation(summary = "Consultar perfil del usuario autenticado", description = "Retorna los datos del perfil del usuario identificado por el token JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Perfil obtenido correctamente",
            content = @Content(schema = @Schema(implementation = PerfilResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<PerfilResponse>> perfil(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.obtenerPerfil(authentication.getName())));
    }

    @Operation(summary = "Actualizar perfil", description = "Modifica el nombre u otros datos del perfil del usuario autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente",
            content = @Content(schema = @Schema(implementation = PerfilResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/perfil")
    public ResponseEntity<ApiResponse<PerfilResponse>> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest req,
            Authentication authentication) {
        PerfilResponse response = usuarioService.actualizarPerfil(authentication.getName(), req);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Historial de votos", description = "Retorna el listado de elecciones en las que el usuario ya participó, con su código de recibo anónimo.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial recuperado correctamente",
            content = @Content(schema = @Schema(implementation = HistorialVotoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/historial-votos")
    public ResponseEntity<ApiResponse<List<HistorialVotoResponse>>> historialVotos(Authentication authentication) {
        List<HistorialVotoResponse> historial = usuarioService.obtenerHistorialVotos(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(historial));
    }

    @Operation(summary = "Desactivar cuenta", description = "Pone la cuenta del usuario autenticado en estado 'inactivo'. La acción es reversible solo por un administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cuenta desactivada correctamente",
            content = @Content(schema = @Schema(implementation = String.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/perfil")
    public ResponseEntity<ApiResponse<String>> desactivarCuenta(Authentication authentication) {
        usuarioService.desactivarCuenta(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cuenta desactivada correctamente."));
    }
}
