package com.votasegura.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Datos para emitir un voto en una elección")
public class EmitirVotoRequest {

    @NotBlank(message = "El ID del candidato es obligatorio")
    @Schema(description = "Identificador único del candidato al que se emite el voto", example = "cand-7f3a2b1c-9e4d", requiredMode = Schema.RequiredMode.REQUIRED)
    private String candidatoId;
}