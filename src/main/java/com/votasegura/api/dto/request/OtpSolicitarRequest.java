package com.votasegura.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Datos para solicitar el envío de un código OTP")
public class OtpSolicitarRequest {

    @Email(message = "El correo no tiene un formato valido")
    @Schema(description = "Correo electrónico destino (requerido si canal=correo)", example = "usuario@correo.com")
    private String correo;

    @Schema(description = "Número de teléfono destino (requerido si canal=sms)", example = "+525512345678")
    private String telefono;

    @NotBlank(message = "El canal es obligatorio")
    @Pattern(regexp = "^(correo|sms)$", message = "El canal debe ser 'correo' o 'sms'")
    @Schema(description = "Canal de entrega del OTP", example = "correo", allowableValues = {"correo", "sms"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String canal;

    @NotBlank(message = "El proposito es obligatorio")
    @Pattern(
        regexp = "^(verificacion_cuenta|segundo_factor|confirmacion_voto|recuperacion_password)$",
        message = "Proposito no valido"
    )
    @Schema(description = "Propósito del OTP solicitado", example = "segundo_factor",
            allowableValues = {"verificacion_cuenta", "segundo_factor", "confirmacion_voto", "recuperacion_password"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String proposito;
}
