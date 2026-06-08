package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(length = 500)
    private String avatar;
    
    @Column(length = 500)
    private String bio;
    
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer followers;
    
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer following;
    
    @Column(name = "max_video_count", columnDefinition = "INT DEFAULT 50")
    private Integer maxVideoCount;
    
    @Column(name = "daily_upload_limit", columnDefinition = "INT DEFAULT 5")
    private Integer dailyUploadLimit;
    
    @Column(name = "max_storage_bytes", columnDefinition = "BIGINT DEFAULT 5368709120")
    private Long maxStorageBytes;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (followers == null) followers = 0;
        if (following == null) following = 0;
        if (maxVideoCount == null) maxVideoCount = 50;
        if (dailyUploadLimit == null) dailyUploadLimit = 5;
        if (maxStorageBytes == null) maxStorageBytes = 5368709120L;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
