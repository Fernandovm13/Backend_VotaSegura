package com.votasegura.api.dto.response;

import com.votasegura.api.model.RegistroVoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialVotoResponse {
    private String eleccionId;
    private String eleccionTitulo;
    private String grupoNombre;
    private String grupoId;
    private String codigoRecibo;
    private LocalDateTime emitidoEn;

    public static HistorialVotoResponse from(RegistroVoto rv) {
        return HistorialVotoResponse.builder()
                .eleccionId(rv.getEleccion().getId())
                .eleccionTitulo(rv.getEleccion().getTitulo())
                .grupoNombre(rv.getEleccion().getGrupo().getNombre())
                .grupoId(rv.getEleccion().getGrupo().getId())
                .codigoRecibo(rv.getCodigoRecibo())
                .emitidoEn(rv.getEmitidoEn())
                .build();
    }
}
