package com.votasegura.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de un grupo de votación")
public class GrupoResponse {

    @Schema(description = "Identificador único del grupo (UUID)", example = "grp-9f8e7d6c-5b4a")
    private String id;

    @Schema(description = "Nombre del grupo", example = "Consejo Estudiantil 2024")
    private String name;

    @Schema(description = "Código único de invitación al grupo (6 caracteres alfanuméricos)", example = "GRP4X2")
    private String code;

    @Schema(description = "Descripción del grupo", example = "Grupo para elecciones del consejo estudiantil")
    private String description;

    @Schema(description = "Rol del usuario actual en el grupo", example = "ADMIN", allowableValues = {"ADMIN", "MIEMBRO"})
    private String role;

    @Schema(description = "Número total de elecciones creadas en el grupo", example = "3")
    private long electionsCount;

    @Schema(description = "Número total de miembros en el grupo", example = "120")
    private long membersCount;

    @Schema(description = "Fecha y hora de creación del grupo", example = "2024-10-01T09:30:00")
    private LocalDateTime creadoEn;
}