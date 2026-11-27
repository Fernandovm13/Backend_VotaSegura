package com.votasegura.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credenciales para autenticación de usuario")
public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    @Schema(description = "Correo electrónico registrado del usuario", example = "usuario@correo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Schema(description = "Contraseña del usuario (mínimo 12 caracteres)", example = "MiPass@2024!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
