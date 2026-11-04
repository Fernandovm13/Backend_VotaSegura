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
public class GrupoResponse {
    private String id;
    private String name;
    private String code;
    private String description;
    private String role;
    private long electionsCount;
    private long membersCount;
    private LocalDateTime creadoEn;
}