package com.votasegura.api.controller;

import com.votasegura.api.dto.request.CrearEleccionRequest;
import com.votasegura.api.dto.request.CrearGrupoRequest;
import com.votasegura.api.dto.request.UnirseGrupoRequest;
import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.EleccionResponse;
import com.votasegura.api.dto.response.GrupoResponse;
import com.votasegura.api.service.EleccionService;
import com.votasegura.api.service.GrupoService;
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

@Tag(name = "Grupos", description = "Creación, consulta, actualización, eliminación de grupos y gestión de miembros")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupoService;
    private final EleccionService eleccionService;

    @Operation(summary = "Crear un grupo", description = "Crea un nuevo grupo de votación. Genera automáticamente un código alfanumérico único de acceso.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Grupo creado correctamente",
            content = @Content(schema = @Schema(implementation = GrupoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Nombre inválido o datos faltantes",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<GrupoResponse>> crearGrupo(
            @Valid @RequestBody CrearGrupoRequest req,
            Authentication auth) {
        GrupoResponse response = grupoService.crearGrupo(auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "Listar mis grupos", description = "Retorna todos los grupos donde el usuario autenticado es miembro o administrador.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de grupos obtenida correctamente",
            content = @Content(schema = @Schema(implementation = GrupoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token de acceso inválido o ausente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<GrupoResponse>>> listarMisGrupos(Authentication auth) {
        List<GrupoResponse> response = grupoService.listarMisGrupos(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Unirse a un grupo", description = "Permite al usuario unirse a un grupo existente mediante su código de acceso corto (ej: K7P-2QX).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario unido al grupo correctamente",
            content = @Content(schema = @Schema(implementation = GrupoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Código de grupo no encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El usuario ya es miembro del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/unirse")
    public ResponseEntity<ApiResponse<GrupoResponse>> unirseAGrupo(
            @Valid @RequestBody UnirseGrupoRequest req,
            Authentication auth) {
        GrupoResponse response = grupoService.unirseAGrupo(auth.getName(), req.getCodigo());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Obtener detalle de un grupo", description = "Retorna la información completa de un grupo específico. El usuario debe ser miembro.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detalle del grupo obtenido",
            content = @Content(schema = @Schema(implementation = GrupoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es miembro del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Grupo no encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{grupoId}")
    public ResponseEntity<ApiResponse<GrupoResponse>> obtenerGrupo(
            @PathVariable String grupoId,
            Authentication auth) {
        GrupoResponse response = grupoService.obtenerGrupo(grupoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Listar elecciones del grupo", description = "Retorna todas las elecciones (programadas, activas y cerradas) pertenecientes al grupo.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de elecciones obtenida",
            content = @Content(schema = @Schema(implementation = EleccionResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es miembro del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{grupoId}/elecciones")
    public ResponseEntity<ApiResponse<List<EleccionResponse>>> listarEleccionesDeGrupo(
            @PathVariable String grupoId,
            Authentication auth) {
        List<EleccionResponse> response = eleccionService.listarEleccionesDeGrupo(grupoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Crear elección dentro de un grupo", description = "Crea una nueva elección con candidatos dentro del grupo. Solo el administrador del grupo puede ejecutar esta acción.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Elección creada correctamente",
            content = @Content(schema = @Schema(implementation = EleccionResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{grupoId}/elecciones")
    public ResponseEntity<ApiResponse<EleccionResponse>> crearEleccion(
            @PathVariable String grupoId,
            @Valid @RequestBody CrearEleccionRequest req,
            Authentication auth) {
        EleccionResponse response = eleccionService.crearEleccion(grupoId, auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "Actualizar un grupo", description = "Modifica el nombre o descripción de un grupo existente. Solo el administrador del grupo puede ejecutar esta acción.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Grupo actualizado correctamente",
            content = @Content(schema = @Schema(implementation = GrupoResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{grupoId}")
    public ResponseEntity<ApiResponse<GrupoResponse>> actualizarGrupo(
            @PathVariable String grupoId,
            @Valid @RequestBody CrearGrupoRequest req,
            Authentication auth) {
        GrupoResponse response = grupoService.actualizarGrupo(grupoId, auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Eliminar un grupo", description = "Elimina permanentemente el grupo, todas sus elecciones, candidatos y registros de votos.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Grupo eliminado correctamente",
            content = @Content(schema = @Schema(implementation = String.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario no es administrador del grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{grupoId}")
    public ResponseEntity<ApiResponse<String>> eliminarGrupo(
            @PathVariable String grupoId,
            Authentication auth) {
        grupoService.eliminarGrupo(grupoId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Grupo eliminado correctamente."));
    }

    @Operation(summary = "Eliminar miembro del grupo", description = "Expulsa a un miembro del grupo o permite al propio usuario salir. Solo el administrador puede expulsar a otros.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Miembro removido correctamente",
            content = @Content(schema = @Schema(implementation = String.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permisos para expulsar a este miembro",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Miembro no encontrado en el grupo",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{grupoId}/miembros/{usuarioId}")
    public ResponseEntity<ApiResponse<String>> eliminarMiembro(
            @PathVariable String grupoId,
            @PathVariable String usuarioId,
            Authentication auth) {
        grupoService.eliminarMiembro(grupoId, usuarioId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Miembro removido correctamente del grupo."));
    }
}