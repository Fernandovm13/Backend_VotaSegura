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

@Tag(name = "Elecciones", description = "Gestión de elecciones, candidatos, emisión de voto anónimo y consulta de resultados")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/elecciones")
@RequiredArgsConstructor
public class EleccionController {

    private final EleccionService eleccionService;

    @Operation(summary = "Obtener detalle de una elección", description = "Retorna la información completa de la elección con su lista de candidatos e indica si el usuario ya votó.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detalle de la elección obtenido",
            content = @Content(schema = @Schema(implementation = EleccionResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es miembro del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Elección no encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{eleccionId}")
    public ResponseEntity<ApiResponse<EleccionResponse>> obtenerEleccion(
            @PathVariable String eleccionId,
            Authentication auth) {
        EleccionResponse response = eleccionService.obtenerEleccion(eleccionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Agregar candidato a una elección", description = "Añade un nuevo candidato a una elección existente. Solo el administrador del grupo puede ejecutar esta acción.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Candidato agregado correctamente",
            content = @Content(schema = @Schema(implementation = CandidatoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos del candidato inválidos o nombre duplicado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{eleccionId}/candidatos")
    public ResponseEntity<ApiResponse<CandidatoResponse>> agregarCandidato(
            @PathVariable String eleccionId,
            @Valid @RequestBody CandidatoItemRequest req,
            Authentication auth) {
        CandidatoResponse response = eleccionService.agregarCandidato(eleccionId, auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "Cambiar estado de una elección", description = "Permite al administrador cambiar el estado manualmente a 'activa' o 'cerrada'.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado cambiado correctamente",
            content = @Content(schema = @Schema(implementation = EleccionResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{eleccionId}/estado")
    public ResponseEntity<ApiResponse<EleccionResponse>> cambiarEstado(
            @PathVariable String eleccionId,
            @Valid @RequestBody CambiarEstadoRequest req,
            Authentication auth) {
        EleccionResponse response = eleccionService.cambiarEstado(eleccionId, auth.getName(), req.getEstado());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Emitir voto anónimo", description = "Registra el voto del usuario de forma anónima. El sistema garantiza que no existe ninguna relación directa entre el votante y el candidato elegido en la base de datos.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Voto emitido correctamente. Se genera un código de recibo anónimo.",
            content = @Content(schema = @Schema(implementation = VotoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "La elección no está activa o los datos son inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es miembro del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El usuario ya emitió su voto en esta elección (voto duplicado)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{eleccionId}/votos")
    public ResponseEntity<ApiResponse<VotoResponse>> emitirVoto(
            @PathVariable String eleccionId,
            @Valid @RequestBody EmitirVotoRequest req,
            Authentication auth) {
        VotoResponse response = eleccionService.emitirVoto(eleccionId, auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "Consultar resultados de una elección", description = "Retorna el conteo total de votos por candidato y determina el ganador o declara empate. Solo visible cuando la elección está cerrada.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resultados obtenidos correctamente",
            content = @Content(schema = @Schema(implementation = ResultadosEleccionResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es miembro del grupo o la elección aún está activa",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{eleccionId}/resultados")
    public ResponseEntity<ApiResponse<ResultadosEleccionResponse>> obtenerResultados(
            @PathVariable String eleccionId,
            Authentication auth) {
        ResultadosEleccionResponse response = eleccionService.obtenerResultados(eleccionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Actualizar una elección", description = "Modifica el título, descripción o fecha de fin de una elección. Solo el administrador del grupo puede ejecutar esta acción.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Elección actualizada correctamente",
            content = @Content(schema = @Schema(implementation = EleccionResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{eleccionId}")
    public ResponseEntity<ApiResponse<EleccionResponse>> actualizarEleccion(
            @PathVariable String eleccionId,
            @Valid @RequestBody CrearEleccionRequest req,
            Authentication auth) {
        EleccionResponse response = eleccionService.actualizarEleccion(eleccionId, auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Eliminar una elección", description = "Elimina permanentemente la elección junto con todos sus candidatos y registros de votos.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Elección eliminada correctamente",
            content = @Content(schema = @Schema(implementation = String.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{eleccionId}")
    public ResponseEntity<ApiResponse<String>> eliminarEleccion(
            @PathVariable String eleccionId,
            Authentication auth) {
        eleccionService.eliminarEleccion(eleccionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Elección eliminada correctamente."));
    }

    @Operation(summary = "Eliminar un candidato", description = "Elimina un candidato específico de la elección. Solo el administrador del grupo puede ejecutar esta acción.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Candidato eliminado correctamente",
            content = @Content(schema = @Schema(implementation = String.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Candidato no encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{eleccionId}/candidatos/{candidatoId}")
    public ResponseEntity<ApiResponse<String>> eliminarCandidato(
            @PathVariable String eleccionId,
            @PathVariable String candidatoId,
            Authentication auth) {
        eleccionService.eliminarCandidato(eleccionId, candidatoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Candidato eliminado correctamente."));
    }
}

