package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_drafts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "video_file_name", length = 200)
    private String videoFileName;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer duration;
    
    @Column(name = "file_size", columnDefinition = "BIGINT DEFAULT 0")
    private Long fileSize;
    
    @Column(name = "tags_text", columnDefinition = "TEXT")
    private String tagsText;

    @Column(name = "file_status", columnDefinition = "VARCHAR(20) DEFAULT 'not_uploaded'")
    private String fileStatus;

    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'draft'")
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
        if (fileSize == null) fileSize = 0L;
        if (fileStatus == null) fileStatus = "not_uploaded";
        if (status == null) status = "draft";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
