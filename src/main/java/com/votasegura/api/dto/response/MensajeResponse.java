package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponse {
    private String mensaje;
    private LocalDateTime invalidadoEn;

    public MensajeResponse(String mensaje) {
        this.mensaje = mensaje;
    }
}
