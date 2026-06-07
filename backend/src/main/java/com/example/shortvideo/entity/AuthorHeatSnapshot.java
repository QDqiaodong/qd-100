package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "author_heat_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorHeatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 50)
    private String authorName;

    @Column(length = 500)
    private String avatar;

    @Column(length = 500)
    private String bio;

    @Column(name = "heat_score", columnDefinition = "DOUBLE DEFAULT 0")
    private Double heatScore;

    @Column(name = "video_count", columnDefinition = "INT DEFAULT 0")
    private Integer videoCount;

    @Column(name = "total_view_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long totalViewCount;

    @Column(name = "total_like_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long totalLikeCount;

    @Column(name = "total_favorite_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long totalFavoriteCount;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer followers;

    @Column(name = "rank_order", nullable = false)
    private Integer rank;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (heatScore == null) heatScore = 0.0;
        if (videoCount == null) videoCount = 0;
        if (totalViewCount == null) totalViewCount = 0L;
        if (totalLikeCount == null) totalLikeCount = 0L;
        if (totalFavoriteCount == null) totalFavoriteCount = 0L;
        if (followers == null) followers = 0;
    }
}
