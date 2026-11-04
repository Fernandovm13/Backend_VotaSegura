package com.votasegura.api.controller;

import com.votasegura.api.dto.request.CrearEleccionRequest;
import com.votasegura.api.dto.request.CrearGrupoRequest;
import com.votasegura.api.dto.request.UnirseGrupoRequest;
import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.EleccionResponse;
import com.votasegura.api.dto.response.GrupoResponse;
import com.votasegura.api.service.EleccionService;
import com.votasegura.api.service.GrupoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupoService;
    private final EleccionService eleccionService;

    /**
     * Crear un grupo/tablón nuevo y genera su código de unión
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GrupoResponse>> crearGrupo(
            @Valid @RequestBody CrearGrupoRequest req,
            Authentication auth) {
        GrupoResponse response = grupoService.crearGrupo(auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Listar todos los grupos a los que pertenece el usuario autenticado
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GrupoResponse>>> listarMisGrupos(Authentication auth) {
        List<GrupoResponse> response = grupoService.listarMisGrupos(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Unirse a un grupo mediante su código de unión corto (ej: K7P-2QX)
     */
    @PostMapping("/unirse")
    public ResponseEntity<ApiResponse<GrupoResponse>> unirseAGrupo(
            @Valid @RequestBody UnirseGrupoRequest req,
            Authentication auth) {
        GrupoResponse response = grupoService.unirseAGrupo(auth.getName(), req.getCodigo());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Obtener el detalle de un grupo
     */
    @GetMapping("/{grupoId}")
    public ResponseEntity<ApiResponse<GrupoResponse>> obtenerGrupo(
            @PathVariable String grupoId,
            Authentication auth) {
        GrupoResponse response = grupoService.obtenerGrupo(grupoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Listar las elecciones pertenecientes a un grupo
     */
    @GetMapping("/{grupoId}/elecciones")
    public ResponseEntity<ApiResponse<List<EleccionResponse>>> listarEleccionesDeGrupo(
            @PathVariable String grupoId,
            Authentication auth) {
        List<EleccionResponse> response = eleccionService.listarEleccionesDeGrupo(grupoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Crear una nueva elección con sus candidatos dentro de un grupo
     */
    @PostMapping("/{grupoId}/elecciones")
    public ResponseEntity<ApiResponse<EleccionResponse>> crearEleccion(
            @PathVariable String grupoId,
            @Valid @RequestBody CrearEleccionRequest req,
            Authentication auth) {
        EleccionResponse response = eleccionService.crearEleccion(grupoId, auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Actualizar los datos de un grupo (Solo administrador)
     */
    @PutMapping("/{grupoId}")
    public ResponseEntity<ApiResponse<GrupoResponse>> actualizarGrupo(
            @PathVariable String grupoId,
            @Valid @RequestBody CrearGrupoRequest req,
            Authentication auth) {
        GrupoResponse response = grupoService.actualizarGrupo(grupoId, auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Eliminar un grupo completo y sus elecciones (Solo administrador)
     */
    @DeleteMapping("/{grupoId}")
    public ResponseEntity<ApiResponse<String>> eliminarGrupo(
            @PathVariable String grupoId,
            Authentication auth) {
        grupoService.eliminarGrupo(grupoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Grupo eliminado correctamente."));
    }

    /**
     * Expulsar a un miembro o salir del grupo
     */
    @DeleteMapping("/{grupoId}/miembros/{usuarioId}")
    public ResponseEntity<ApiResponse<String>> eliminarMiembro(
            @PathVariable String grupoId,
            @PathVariable String usuarioId,
            Authentication auth) {
        grupoService.eliminarMiembro(grupoId, usuarioId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Miembro removido correctamente del grupo."));
    }
}