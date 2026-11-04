package com.votasegura.api.controller;

import com.votasegura.api.dto.request.CambiarEstadoRequest;
import com.votasegura.api.dto.request.CandidatoItemRequest;
import com.votasegura.api.dto.request.CrearEleccionRequest;
import com.votasegura.api.dto.request.EmitirVotoRequest;
import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.CandidatoResponse;
import com.votasegura.api.dto.response.EleccionResponse;
import com.votasegura.api.dto.response.ResultadosEleccionResponse;
import com.votasegura.api.dto.response.VotoResponse;
import com.votasegura.api.service.EleccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/elecciones")
@RequiredArgsConstructor
public class EleccionController {

    private final EleccionService eleccionService;

    /**
     * Consultar el detalle de una elección con su lista de candidatos
     */
    @GetMapping("/{eleccionId}")
    public ResponseEntity<ApiResponse<EleccionResponse>> obtenerEleccion(
            @PathVariable String eleccionId,
            Authentication auth) {
        EleccionResponse response = eleccionService.obtenerEleccion(eleccionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Agregar un candidato a una elección existente
     */
    @PostMapping("/{eleccionId}/candidatos")
    public ResponseEntity<ApiResponse<CandidatoResponse>> agregarCandidato(
            @PathVariable String eleccionId,
            @Valid @RequestBody CandidatoItemRequest req,
            Authentication auth) {
        CandidatoResponse response = eleccionService.agregarCandidato(eleccionId, auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Cambiar el estado de una elección (activa / cerrada)
     */
    @PatchMapping("/{eleccionId}/estado")
    public ResponseEntity<ApiResponse<EleccionResponse>> cambiarEstado(
            @PathVariable String eleccionId,
            @Valid @RequestBody CambiarEstadoRequest req,
            Authentication auth) {
        EleccionResponse response = eleccionService.cambiarEstado(eleccionId, auth.getName(), req.getEstado());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Emitir un voto en una elección (secreto, validando 1 voto por usuario)
     */
    @PostMapping("/{eleccionId}/votos")
    public ResponseEntity<ApiResponse<VotoResponse>> emitirVoto(
            @PathVariable String eleccionId,
            @Valid @RequestBody EmitirVotoRequest req,
            Authentication auth) {
        VotoResponse response = eleccionService.emitirVoto(eleccionId, auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Consultar los resultados y conteo de votos de una elección
     */
    @GetMapping("/{eleccionId}/resultados")
    public ResponseEntity<ApiResponse<ResultadosEleccionResponse>> obtenerResultados(
            @PathVariable String eleccionId,
            Authentication auth) {
        ResultadosEleccionResponse response = eleccionService.obtenerResultados(eleccionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Actualizar los datos de una elección existente (Solo administrador)
     */
    @PutMapping("/{eleccionId}")
    public ResponseEntity<ApiResponse<EleccionResponse>> actualizarEleccion(
            @PathVariable String eleccionId,
            @Valid @RequestBody CrearEleccionRequest req,
            Authentication auth) {
        EleccionResponse response = eleccionService.actualizarEleccion(eleccionId, auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Eliminar una elección completa y sus votos/candidatos (Solo administrador)
     */
    @DeleteMapping("/{eleccionId}")
    public ResponseEntity<ApiResponse<String>> eliminarEleccion(
            @PathVariable String eleccionId,
            Authentication auth) {
        eleccionService.eliminarEleccion(eleccionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Elección eliminada correctamente."));
    }

    /**
     * Eliminar un candidato específico de una elección (Solo administrador)
     */
    @DeleteMapping("/{eleccionId}/candidatos/{candidatoId}")
    public ResponseEntity<ApiResponse<String>> eliminarCandidato(
            @PathVariable String eleccionId,
            @PathVariable String candidatoId,
            Authentication auth) {
        eleccionService.eliminarCandidato(eleccionId, candidatoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Candidato eliminado correctamente."));
    }
}
