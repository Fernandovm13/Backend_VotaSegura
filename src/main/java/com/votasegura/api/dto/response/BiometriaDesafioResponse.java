package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiometriaDesafioResponse {
    private String challenge;
    private String rpId;
    private String userId;
    private List<String> algoritmosPermitidos;
    private int timeout;
}
