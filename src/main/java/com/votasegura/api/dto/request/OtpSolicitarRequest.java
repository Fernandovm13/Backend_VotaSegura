package com.votasegura.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpSolicitarRequest {

    @Email(message = "El correo no tiene un formato valido")
    private String correo;

    private String telefono;

    @NotBlank(message = "El canal es obligatorio")
    @Pattern(regexp = "^(correo|sms)$", message = "El canal debe ser 'correo' o 'sms'")
    private String canal;

    @NotBlank(message = "El proposito es obligatorio")
    @Pattern(
        regexp = "^(verificacion_cuenta|segundo_factor|confirmacion_voto|recuperacion_password)$",
        message = "Proposito no valido"
    )
    private String proposito;
}
