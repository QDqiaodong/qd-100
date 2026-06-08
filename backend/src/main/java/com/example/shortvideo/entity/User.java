package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    @Column(name = "penalty_level", columnDefinition = "VARCHAR(20) DEFAULT 'normal'")
    private String penaltyLevel;

    @Column(name = "active_penalty_points", columnDefinition = "INT DEFAULT 0")
    private Integer activePenaltyPoints;

    @Column(name = "total_violation_count", columnDefinition = "INT DEFAULT 0")
    private Integer totalViolationCount;

    @Column(name = "last_violation_at")
    private LocalDateTime lastViolationAt;

    @Column(name = "content_visibility", columnDefinition = "VARCHAR(20) DEFAULT 'public'")
    private String contentVisibility;

    @Column(name = "audit_priority", columnDefinition = "VARCHAR(20) DEFAULT 'normal'")
    private String auditPriority;

    @Column(name = "penalty_expires_at")
    private LocalDateTime penaltyExpiresAt;
    
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
        if (penaltyLevel == null) penaltyLevel = "normal";
        if (activePenaltyPoints == null) activePenaltyPoints = 0;
        if (totalViolationCount == null) totalViolationCount = 0;
        if (contentVisibility == null) contentVisibility = "public";
        if (auditPriority == null) auditPriority = "normal";
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static class PenaltySummary {
        private final String penaltyLevel;
        private final Integer activePenaltyPoints;
        private final Integer totalViolationCount;
        private final String auditPriority;
        private final String contentVisibility;
        private final LocalDateTime penaltyExpiresAt;
        private final Integer weekViolationCount;
        private final Integer monthViolationCount;
        private final List<Object[]> violationTypeStats;

        public PenaltySummary(String penaltyLevel, Integer activePenaltyPoints,
                              Integer totalViolationCount, String auditPriority,
                              String contentVisibility, LocalDateTime penaltyExpiresAt,
                              Integer weekViolationCount, Integer monthViolationCount,
                              List<Object[]> violationTypeStats) {
            this.penaltyLevel = penaltyLevel;
            this.activePenaltyPoints = activePenaltyPoints;
            this.totalViolationCount = totalViolationCount;
            this.auditPriority = auditPriority;
            this.contentVisibility = contentVisibility;
            this.penaltyExpiresAt = penaltyExpiresAt;
            this.weekViolationCount = weekViolationCount;
            this.monthViolationCount = monthViolationCount;
            this.violationTypeStats = violationTypeStats;
        }

        public String getPenaltyLevel() { return penaltyLevel; }
        public Integer getActivePenaltyPoints() { return activePenaltyPoints; }
        public Integer getTotalViolationCount() { return totalViolationCount; }
        public String getAuditPriority() { return auditPriority; }
        public String getContentVisibility() { return contentVisibility; }
        public LocalDateTime getPenaltyExpiresAt() { return penaltyExpiresAt; }
        public Integer getWeekViolationCount() { return weekViolationCount; }
        public Integer getMonthViolationCount() { return monthViolationCount; }
        public List<Object[]> getViolationTypeStats() { return violationTypeStats; }
    }
}
