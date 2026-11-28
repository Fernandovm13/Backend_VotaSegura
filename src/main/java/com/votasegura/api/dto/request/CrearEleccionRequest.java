package com.votasegura.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Datos para crear una nueva elección dentro de un grupo")
public class CrearEleccionRequest {

    @NotBlank(message = "El título de la elección es obligatorio")
    @Size(min = 3, max = 200, message = "El título debe tener entre 3 y 200 caracteres")
    @Pattern(
        regexp = "^[^<>\"'&;]*$",
        message = "El título no puede contener caracteres HTML especiales (< > \" ' & ;)"
    )
    @Schema(description = "Título descriptivo de la elección", example = "Elección de Presidente 2024", requiredMode = Schema.RequiredMode.REQUIRED)
    private String titulo;

    @Pattern(
        regexp = "^[^<>\"'&;]*$",
        message = "La descripción no puede contener caracteres HTML especiales (< > \" ' & ;)"
    )
    @Schema(description = "Descripción opcional de la elección y sus propósitos", example = "Votación para elegir al presidente del consejo estudiantil")
    private String descripcion;

    @Schema(description = "Fecha y hora de inicio de la elección (ISO 8601)", example = "2024-12-01T08:00:00")
    private String fechaInicio;

    @Schema(description = "Fecha y hora de cierre de la elección (ISO 8601)", example = "2024-12-01T20:00:00")
    private String fechaFin;

    @Valid
    @Schema(description = "Lista de candidatos que participan en la elección")
    private List<CandidatoItemRequest> candidatos = new ArrayList<>();
}
