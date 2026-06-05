package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.VideoMilestone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMilestoneDTO {
    private Long id;
    private Long videoId;
    private String title;
    private String description;
    private Integer timestampSeconds;
    private Integer sortOrder;
    private String createdAt;

    public static VideoMilestoneDTO fromEntity(VideoMilestone milestone) {
        return VideoMilestoneDTO.builder()
                .id(milestone.getId())
                .videoId(milestone.getVideoId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .timestampSeconds(milestone.getTimestampSeconds())
                .sortOrder(milestone.getSortOrder())
                .createdAt(milestone.getCreatedAt().toString())
                .build();
    }
}
