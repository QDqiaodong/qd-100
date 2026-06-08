package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.VideoAppeal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAppealDTO {
    private Long id;
    private Long videoId;
    private Long userId;
    private String appealType;
    private String content;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private String reviewResult;
    private String createdAt;
    private String updatedAt;
    private VideoDTO video;
    private UserDTO user;

    public static VideoAppealDTO fromEntity(VideoAppeal appeal) {
        return VideoAppealDTO.builder()
                .id(appeal.getId())
                .videoId(appeal.getVideoId())
                .userId(appeal.getUserId())
                .appealType(appeal.getAppealType())
                .content(appeal.getContent())
                .status(appeal.getStatus())
                .reviewerId(appeal.getReviewerId())
                .reviewComment(appeal.getReviewComment())
                .reviewResult(appeal.getReviewResult())
                .createdAt(appeal.getCreatedAt() != null ? appeal.getCreatedAt().toString() : null)
                .updatedAt(appeal.getUpdatedAt() != null ? appeal.getUpdatedAt().toString() : null)
                .build();
    }
}
