package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.TagHeatSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagHeatSnapshotDTO {
    private Long tagId;
    private String tagName;
    private Double heatScore;
    private Integer videoCount;
    private Long viewCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long commentCount;
    private Integer rank;

    public static TagHeatSnapshotDTO fromEntity(TagHeatSnapshot entity) {
        return TagHeatSnapshotDTO.builder()
                .tagId(entity.getTagId())
                .tagName(entity.getTagName())
                .heatScore(entity.getHeatScore())
                .videoCount(entity.getVideoCount())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .favoriteCount(entity.getFavoriteCount())
                .commentCount(entity.getCommentCount())
                .rank(entity.getRank())
                .build();
    }
}
