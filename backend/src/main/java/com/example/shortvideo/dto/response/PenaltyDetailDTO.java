package com.example.shortvideo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyDetailDTO {
    private String penaltyLevel;
    private Integer activePenaltyPoints;
    private Integer totalViolationCount;
    private String auditPriority;
    private String contentVisibility;
    private String penaltyExpiresAt;
    private Integer weekViolationCount;
    private Integer monthViolationCount;
    private List<ViolationTypeStatDTO> violationTypeStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViolationTypeStatDTO {
        private String violationType;
        private Long count;
    }
}
