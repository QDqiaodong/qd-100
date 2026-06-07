package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    private Long id;
    private String title;
    private String description;
    private List<String> tags;
    private String coverUrl;
    private String videoUrl;
    private Integer duration;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private Integer commentCount;
    private Integer shareCount;
    private Double heatScore;
    private String status;
    private UserDTO author;
    private String createdAt;
    
    public static VideoDTO fromEntity(Video video) {
        return VideoDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .coverUrl(video.getCoverUrl())
                .videoUrl(video.getVideoUrl())
                .duration(video.getDuration())
                .likeCount(video.getLikeCount())
                .favoriteCount(video.getFavoriteCount())
                .viewCount(video.getViewCount())
                .commentCount(video.getCommentCount())
                .shareCount(video.getShareCount())
                .heatScore(video.getHeatScore())
                .status(video.getStatus())
                .createdAt(video.getCreatedAt().toString())
                .build();
    }
}
