package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tag_heat_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagHeatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;

    @Column(name = "heat_score", columnDefinition = "DOUBLE DEFAULT 0")
    private Double heatScore;

    @Column(name = "video_count", columnDefinition = "INT DEFAULT 0")
    private Integer videoCount;

    @Column(name = "view_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long viewCount;

    @Column(name = "like_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long likeCount;

    @Column(name = "favorite_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long favoriteCount;

    @Column(name = "comment_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long commentCount;

    @Column(name = "rank_order", nullable = false)
    private Integer rank;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (heatScore == null) heatScore = 0.0;
        if (videoCount == null) videoCount = 0;
        if (viewCount == null) viewCount = 0L;
        if (likeCount == null) likeCount = 0L;
        if (favoriteCount == null) favoriteCount = 0L;
        if (commentCount == null) commentCount = 0L;
    }
}
