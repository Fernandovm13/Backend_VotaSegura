package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadosEleccionResponse {
    private String eleccionId;
    private String titulo;
    private String grupoNombre;
    private String estado;
    private long totalVotos;
    private CandidatoResponse ganador;
    private boolean empate;
    @Builder.Default
    private List<CandidatoResponse> empatados = new ArrayList<>();
    @Builder.Default
    private List<CandidatoResponse> candidatos = new ArrayList<>();
}