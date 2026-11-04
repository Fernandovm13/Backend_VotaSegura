package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EleccionResponse {
    private String id;
    private String groupId;
    private String groupName;
    private String title;
    private String description;
    private String status;
    private boolean hasVoted;
    private long totalVoters;
    private long votesCast;
    private String startDate;
    private String endDate;
    @Builder.Default
    private List<CandidatoResponse> candidates = new ArrayList<>();
}