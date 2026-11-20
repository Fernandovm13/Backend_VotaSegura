package com.votasegura.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Datos para verificar un código OTP previamente enviado")
public class OtpVerificarRequest {

    @NotBlank(message = "El otpId es obligatorio")
    @Schema(description = "Identificador único del OTP a verificar (retornado al solicitar)", example = "otp-5a3f2c1d-8e6b", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpId;

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo debe ser de 6 digitos numericos")
    @Schema(description = "Código OTP de 6 dígitos numéricos recibido por el usuario", example = "482913", requiredMode = Schema.RequiredMode.REQUIRED)
    private String codigo;
}
