package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.WatchProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchProgressDTO {
    private Long id;
    private Long userId;
    private VideoDTO video;
    private Integer currentTime;
    private Boolean isCompleted;
    private String updatedAt;
    
    public static WatchProgressDTO fromEntity(WatchProgress progress, VideoDTO video) {
        return WatchProgressDTO.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .video(video)
                .currentTime(progress.getCurrentTime())
                .isCompleted(progress.getIsCompleted())
                .updatedAt(progress.getUpdatedAt().toString())
                .build();
    }
}
