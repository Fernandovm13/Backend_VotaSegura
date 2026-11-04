package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BiometriaDesafioRequest {
    @NotBlank(message = "El tipoAutenticador es obligatorio")
    @Pattern(
        regexp = "^(huella|rostro|platform_authenticator)$",
        message = "Tipo de autenticador no soportado"
    )
    private String tipoAutenticador;

    private String domain;
}
