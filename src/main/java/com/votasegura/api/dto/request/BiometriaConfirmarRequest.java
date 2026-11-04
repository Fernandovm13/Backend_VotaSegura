package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BiometriaConfirmarRequest {
    @NotBlank private String challenge;
    @NotBlank private String credentialId;
    @NotBlank private String clavePublica;
    @NotBlank private String attestationObject;
    @NotBlank private String clientDataJSON;
    private String tipoAutenticador;
    private String dispositivo;
}
