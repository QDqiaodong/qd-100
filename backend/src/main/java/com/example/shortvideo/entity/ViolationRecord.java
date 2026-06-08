package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "violation_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "video_id")
    private Long videoId;

    @Column(name = "violation_type", nullable = false, length = 50)
    private String violationType;

    @Column(name = "violation_reason", columnDefinition = "TEXT")
    private String violationReason;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "is_repeat_offense", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isRepeatOffense;

    @Column(name = "penalty_points", columnDefinition = "INT DEFAULT 1")
    private Integer penaltyPoints;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (severity == null) severity = "minor";
        if (penaltyPoints == null) penaltyPoints = 1;
        if (isRepeatOffense == null) isRepeatOffense = false;
    }
}
