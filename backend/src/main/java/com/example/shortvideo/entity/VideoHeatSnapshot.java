package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_heat_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoHeatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", length = 50)
    private String authorName;

    @Column(name = "heat_score", columnDefinition = "DOUBLE DEFAULT 0")
    private Double heatScore;

    @Column(name = "view_count", columnDefinition = "INT DEFAULT 0")
    private Integer viewCount;

    @Column(name = "like_count", columnDefinition = "INT DEFAULT 0")
    private Integer likeCount;

    @Column(name = "favorite_count", columnDefinition = "INT DEFAULT 0")
    private Integer favoriteCount;

    @Column(name = "comment_count", columnDefinition = "INT DEFAULT 0")
    private Integer commentCount;

    @Column(name = "share_count", columnDefinition = "INT DEFAULT 0")
    private Integer shareCount;

    @Column(name = "rank_order", nullable = false)
    private Integer rank;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (heatScore == null) heatScore = 0.0;
        if (viewCount == null) viewCount = 0;
        if (likeCount == null) likeCount = 0;
        if (favoriteCount == null) favoriteCount = 0;
        if (commentCount == null) commentCount = 0;
        if (shareCount == null) shareCount = 0;
    }
}
