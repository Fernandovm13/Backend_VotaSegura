package com.votasegura.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información completa de una elección")
public class EleccionResponse {

    @Schema(description = "Identificador único de la elección (UUID)", example = "elec-1a2b3c4d-5e6f")
    private String id;

    @Schema(description = "Identificador del grupo al que pertenece la elección", example = "grp-9f8e7d6c-5b4a")
    private String groupId;

    @Schema(description = "Nombre del grupo al que pertenece la elección", example = "Consejo Estudiantil 2024")
    private String groupName;

    @Schema(description = "Título de la elección", example = "Elección de Presidente 2024")
    private String title;

    @Schema(description = "Descripción de la elección", example = "Votación para elegir al presidente del consejo")
    private String description;

    @Schema(description = "Estado actual de la elección", example = "ACTIVA", allowableValues = {"PENDIENTE", "ACTIVA", "CERRADA"})
    private String status;

    @Schema(description = "Indica si el usuario actual ya emitió su voto en esta elección", example = "false")
    private boolean hasVoted;

    @Schema(description = "Total de votantes habilitados en el grupo", example = "120")
    private long totalVoters;

    @Schema(description = "Total de votos emitidos hasta el momento", example = "87")
    private long votesCast;

    @Schema(description = "Fecha y hora de inicio de la elección (ISO 8601)", example = "2024-12-01T08:00:00")
    private String startDate;

    @Schema(description = "Fecha y hora de cierre de la elección (ISO 8601)", example = "2024-12-01T20:00:00")
    private String endDate;

    @Builder.Default
    @Schema(description = "Lista de candidatos que participan en la elección")
    private List<CandidatoResponse> candidates = new ArrayList<>();
}