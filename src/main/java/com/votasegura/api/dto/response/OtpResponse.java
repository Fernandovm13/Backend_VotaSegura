package com.votasegura.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta al solicitar el envío de un código OTP")
public class OtpResponse {

    @Schema(description = "Mensaje informativo del resultado de la solicitud", example = "Código OTP enviado exitosamente al correo registrado")
    private String mensaje;

    @Schema(description = "Identificador único del OTP generado (necesario para la verificación)", example = "otp-5a3f2c1d-8e6b")
    private String otpId;

    @Schema(description = "Canal por el que se envió el OTP", example = "correo", allowableValues = {"correo", "sms"})
    private String canal;

    @Schema(description = "Tiempo de expiración del OTP en segundos", example = "300")
    private int expiraEn;
}
