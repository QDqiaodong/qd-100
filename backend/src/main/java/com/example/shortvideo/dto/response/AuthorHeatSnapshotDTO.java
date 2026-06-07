package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.AuthorHeatSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorHeatSnapshotDTO {
    private Long authorId;
    private String authorName;
    private String avatar;
    private String bio;
    private Double heatScore;
    private Integer videoCount;
    private Long totalViewCount;
    private Long totalLikeCount;
    private Long totalFavoriteCount;
    private Integer followers;
    private Integer rank;

    public static AuthorHeatSnapshotDTO fromEntity(AuthorHeatSnapshot entity) {
        return AuthorHeatSnapshotDTO.builder()
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthorName())
                .avatar(entity.getAvatar())
                .bio(entity.getBio())
                .heatScore(entity.getHeatScore())
                .videoCount(entity.getVideoCount())
                .totalViewCount(entity.getTotalViewCount())
                .totalLikeCount(entity.getTotalLikeCount())
                .totalFavoriteCount(entity.getTotalFavoriteCount())
                .followers(entity.getFollowers())
                .rank(entity.getRank())
                .build();
    }
}
