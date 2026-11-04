package com.votasegura.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CrearEleccionRequest {
    @NotBlank(message = "El título de la elección es obligatorio")
    private String titulo;

    private String descripcion;
    private String fechaInicio;
    private String fechaFin;

    @Valid
    private List<CandidatoItemRequest> candidatos = new ArrayList<>();
}