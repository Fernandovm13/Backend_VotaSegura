package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarEstadoRequest {
    @NotBlank(message = "El estado es obligatorio (activa o cerrada)")
    private String estado;
}