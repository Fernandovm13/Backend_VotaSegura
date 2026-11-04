package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RestablecerPasswordRequest {
    @NotBlank(message = "El resetToken es obligatorio")
    private String resetToken;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{12,}$",
        message = "La contrasena debe tener al menos 12 caracteres, una mayuscula, un numero y un caracter especial"
    )
    private String nuevaPassword;
}
