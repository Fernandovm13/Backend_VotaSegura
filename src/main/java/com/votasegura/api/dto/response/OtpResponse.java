package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private String mensaje;
    private String otpId;
    private String canal;
    private int expiraEn;
}
