package com.votasegura.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CandidatoItemRequest {
    @NotBlank(message = "El nombre del candidato es obligatorio")
    private String nombre;
    private String puesto;
    private String descripcion;
    private String avatarUrl;
}