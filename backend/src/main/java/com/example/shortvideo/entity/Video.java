package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "cover_url", length = 500)
    private String coverUrl;
    
    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;
    
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer duration;
    
    @Column(name = "like_count", columnDefinition = "INT DEFAULT 0")
    private Integer likeCount;
    
    @Column(name = "favorite_count", columnDefinition = "INT DEFAULT 0")
    private Integer favoriteCount;
    
    @Column(name = "view_count", columnDefinition = "INT DEFAULT 0")
    private Integer viewCount;
    
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'pending'")
    private String status;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (duration == null) duration = 0;
        if (likeCount == null) likeCount = 0;
        if (favoriteCount == null) favoriteCount = 0;
        if (viewCount == null) viewCount = 0;
        if (status == null) status = "pending";
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
