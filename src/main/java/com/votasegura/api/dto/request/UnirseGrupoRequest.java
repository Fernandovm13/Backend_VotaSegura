package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnirseGrupoRequest {
    @NotBlank(message = "El código de grupo es obligatorio")
    private String codigo;
}