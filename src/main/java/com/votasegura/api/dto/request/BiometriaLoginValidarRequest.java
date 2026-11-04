package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BiometriaLoginValidarRequest {
    @NotBlank private String credentialId;
    @NotBlank private String challenge;
    @NotBlank private String firma;
    @NotBlank private String authenticatorData;
    @NotBlank private String clientDataJSON;
}
