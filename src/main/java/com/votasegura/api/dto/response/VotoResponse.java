package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotoResponse {
    private String receiptCode;
    private String status;
    private LocalDateTime fecha;
    private String electionTitle;
    private String groupName;
    private String mensaje;
}