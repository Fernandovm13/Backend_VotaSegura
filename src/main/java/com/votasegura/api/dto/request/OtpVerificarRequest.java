package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerificarRequest {
    @NotBlank(message = "El otpId es obligatorio")
    private String otpId;

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo debe ser de 6 digitos numericos")
    private String codigo;
}
