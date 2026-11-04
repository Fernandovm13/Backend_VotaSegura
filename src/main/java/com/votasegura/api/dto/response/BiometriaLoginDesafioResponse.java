package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiometriaLoginDesafioResponse {
    private String challenge;
    private List<CredencialInfo> allowCredentials;
    private int timeout;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredencialInfo {
        private String credentialId;
        private String tipo = "public-key";
    }
}
