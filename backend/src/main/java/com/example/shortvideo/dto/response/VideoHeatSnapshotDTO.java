package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.VideoHeatSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoHeatSnapshotDTO {
    private Long videoId;
    private String title;
    private String coverUrl;
    private Long authorId;
    private String authorName;
    private Double heatScore;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer rank;

    public static VideoHeatSnapshotDTO fromEntity(VideoHeatSnapshot entity) {
        return VideoHeatSnapshotDTO.builder()
                .videoId(entity.getVideoId())
                .title(entity.getTitle())
                .coverUrl(entity.getCoverUrl())
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthorName())
                .heatScore(entity.getHeatScore())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .favoriteCount(entity.getFavoriteCount())
                .commentCount(entity.getCommentCount())
                .shareCount(entity.getShareCount())
                .rank(entity.getRank())
                .build();
    }
}
