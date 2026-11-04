package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank(message = "El refreshToken es obligatorio")
    private String refreshToken;
}
