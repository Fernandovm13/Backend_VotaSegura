package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredencialRegistradaResponse {
    private String credentialId;
    private String tipoAutenticador;
    private String dispositivo;
    private LocalDateTime registradoEn;
}
