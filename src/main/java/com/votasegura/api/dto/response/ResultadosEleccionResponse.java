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
@Schema(description = "Resultados finales de una elección cerrada")
public class ResultadosEleccionResponse {

    @Schema(description = "Identificador único de la elección", example = "elec-1a2b3c4d-5e6f")
    private String eleccionId;

    @Schema(description = "Título de la elección", example = "Elección de Presidente 2024")
    private String titulo;

    @Schema(description = "Nombre del grupo donde se realizó la elección", example = "Consejo Estudiantil 2024")
    private String grupoNombre;

    @Schema(description = "Estado de la elección", example = "CERRADA", allowableValues = {"PENDIENTE", "ACTIVA", "CERRADA"})
    private String estado;

    @Schema(description = "Total de votos emitidos en la elección", example = "87")
    private long totalVotos;

    @Schema(description = "Candidato ganador (null en caso de empate)")
    private CandidatoResponse ganador;

    @Schema(description = "Indica si la elección terminó en empate", example = "false")
    private boolean empate;

    @Builder.Default
    @Schema(description = "Lista de candidatos empatados (solo se llena cuando empate=true)")
    private List<CandidatoResponse> empatados = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Lista completa de candidatos con sus resultados")
    private List<CandidatoResponse> candidatos = new ArrayList<>();
}