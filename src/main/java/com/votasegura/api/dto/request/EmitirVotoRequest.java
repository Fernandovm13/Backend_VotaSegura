package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmitirVotoRequest {
    @NotBlank(message = "El ID del candidato es obligatorio")
    private String candidatoId;
}