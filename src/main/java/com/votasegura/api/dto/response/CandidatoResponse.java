package com.votasegura.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de un candidato en una elección")
public class CandidatoResponse {

    @Schema(description = "Identificador único del candidato (UUID)", example = "cand-7f3a2b1c-9e4d")
    private String id;

    @Schema(description = "Nombre completo del candidato", example = "Carlos Ramírez")
    private String name;

    @Schema(description = "Puesto o cargo al que aspira", example = "Presidente")
    private String position;

    @Schema(description = "Descripción o propuesta del candidato", example = "Candidato con 5 años de experiencia en gestión estudiantil")
    private String description;

    @Schema(description = "URL del avatar o imagen del candidato", example = "https://cdn.votasegura.com/avatars/carlos.jpg")
    private String avatarUrl;

    @Schema(description = "Número total de votos recibidos (visible solo después del cierre)", example = "45")
    private long votesCount;

    @Schema(description = "Porcentaje de votos sobre el total emitido (visible solo después del cierre)", example = "51.72")
    private double percentage;
}