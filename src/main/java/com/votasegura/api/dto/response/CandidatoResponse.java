package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidatoResponse {
    private String id;
    private String name;
    private String position;
    private String description;
    private String avatarUrl;
    private long votesCount;
    private double percentage;
}