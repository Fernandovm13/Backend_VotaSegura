package com.votasegura.api.dto.request;

import com.votasegura.api.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Datos para registrar un nuevo usuario en el sistema")
public class RegistroRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Schema(description = "Nombre completo del usuario", example = "Ana García López", minLength = 2, maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    @Schema(description = "Correo electrónico único del usuario", example = "ana.garcia@correo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\\\"\\\\|,.<>/?]).{12,}$",
        message = "La contrasena debe tener al menos 12 caracteres, una mayuscula, un numero y un caracter especial"
    )
    @Schema(description = "Contraseña segura: mínimo 12 caracteres, una mayúscula, un número y un carácter especial", example = "Segura@2024!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Rol asignado al usuario en el sistema", example = "VOTANTE", allowableValues = {"VOTANTE", "ADMIN"}, defaultValue = "VOTANTE")
    private Rol rol = Rol.VOTANTE;
}
