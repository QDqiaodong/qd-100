package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.ViolationRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationRecordDTO {
    private Long id;
    private Long userId;
    private Long videoId;
    private String violationType;
    private String violationReason;
    private String severity;
    private Boolean isRepeatOffense;
    private Integer penaltyPoints;
    private String createdAt;
    private String expiresAt;

    public static ViolationRecordDTO fromEntity(ViolationRecord record) {
        return ViolationRecordDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .videoId(record.getVideoId())
                .violationType(record.getViolationType())
                .violationReason(record.getViolationReason())
                .severity(record.getSeverity())
                .isRepeatOffense(record.getIsRepeatOffense())
                .penaltyPoints(record.getPenaltyPoints())
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().toString() : null)
                .expiresAt(record.getExpiresAt() != null ? record.getExpiresAt().toString() : null)
                .build();
    }
}
