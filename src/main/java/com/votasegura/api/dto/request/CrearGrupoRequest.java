package com.votasegura.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para crear un nuevo grupo de votación")
public class CrearGrupoRequest {

    @NotBlank(message = "El nombre del grupo es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
    @Schema(description = "Nombre del grupo (entre 3 y 120 caracteres)", example = "Consejo Estudiantil 2024", minLength = 3, maxLength = 120, requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @Schema(description = "Descripción opcional del grupo y su propósito", example = "Grupo para elecciones del consejo estudiantil universitario")
    private String descripcion;
}