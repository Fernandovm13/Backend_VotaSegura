package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoResponse {
    private String nombreOriginal;
    private String nombreArchivo;
    private String url;
    private String tipo;
    private long tamanioBytes;
}