package com.votasegura.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos de un candidato dentro de una elección")
public class CandidatoItemRequest {

    @NotBlank(message = "El nombre del candidato es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    @Pattern(
        regexp = "^[^<>\"'&;]*$",
        message = "El nombre no puede contener caracteres HTML especiales (< > \" ' & ;)"
    )
    @Schema(description = "Nombre completo del candidato", example = "Carlos Ramírez", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @Pattern(
        regexp = "^[^<>\"'&;]*$",
        message = "El puesto no puede contener caracteres HTML especiales (< > \" ' & ;)"
    )
    @Schema(description = "Puesto o cargo al que aspira el candidato", example = "Presidente")
    private String puesto;

    @Pattern(
        regexp = "^[^<>\"'&;]*$",
        message = "La descripción no puede contener caracteres HTML especiales (< > \" ' & ;)"
    )
    @Schema(description = "Descripción o propuesta del candidato", example = "Candidato con 5 años de experiencia en gestión estudiantil")
    private String descripcion;

    @Schema(description = "URL de la imagen/avatar del candidato", example = "https://cdn.votasegura.com/avatars/carlos.jpg")
    private String avatarUrl;
}
