package com.example.shortvideo.service;

import com.example.shortvideo.entity.User;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.entity.ViolationRecord;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoRepository;
import com.example.shortvideo.repository.ViolationRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContentGovernanceService {

    private final ViolationRecordRepository violationRecordRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    private static final int PENALTY_EXPIRY_DAYS = 30;
    private static final int WATCH_LEVEL_THRESHOLD = 3;
    private static final int STRICT_LEVEL_THRESHOLD = 6;
    private static final int SEVERE_LEVEL_THRESHOLD = 10;
    private static final int REPEAT_OFFENSE_BONUS_POINTS = 1;
    private static final int SIMILAR_CONTENT_WINDOW_DAYS = 7;

    public ContentGovernanceService(ViolationRecordRepository violationRecordRepository,
                                    UserRepository userRepository,
                                    VideoRepository videoRepository) {
        this.violationRecordRepository = violationRecordRepository;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    @Transactional
    public ViolationRecord recordViolation(Long userId, Long videoId, String violationType,
                                           String violationReason, String severity) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        boolean isRepeatOffense = checkRepeatOffense(userId, violationType);

        int basePoints = getPenaltyPointsBySeverity(severity);
        int totalPoints = isRepeatOffense ? basePoints + REPEAT_OFFENSE_BONUS_POINTS : basePoints;

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(PENALTY_EXPIRY_DAYS);

        ViolationRecord record = ViolationRecord.builder()
                .userId(userId)
                .videoId(videoId)
                .violationType(violationType)
                .violationReason(violationReason)
                .severity(severity)
                .isRepeatOffense(isRepeatOffense)
                .penaltyPoints(totalPoints)
                .expiresAt(expiresAt)
                .build();

        record = violationRecordRepository.save(record);

        updateUserPenaltyStatus(user, totalPoints);

        if (videoId != null) {
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video != null) {
                video.setViolationType(violationType);
                video.setRejectReason(violationReason);
                videoRepository.save(video);
            }
        }

        return record;
    }

    private int getPenaltyPointsBySeverity(String severity) {
        return switch (severity) {
            case "trivial" -> 1;
            case "minor" -> 2;
            case "moderate" -> 3;
            case "major" -> 5;
            case "severe" -> 8;
            case "critical" -> 12;
            default -> 2;
        };
    }

    public boolean checkRepeatOffense(Long userId, String violationType) {
        LocalDateTime since = LocalDateTime.now().minusDays(SIMILAR_CONTENT_WINDOW_DAYS);
        Long count = violationRecordRepository.countByUserIdAndViolationTypeSince(userId, violationType, since);
        return count != null && count > 0;
    }

    public boolean checkSimilarContentViolation(Long userId, String title, String description) {
        LocalDateTime since = LocalDateTime.now().minusDays(SIMILAR_CONTENT_WINDOW_DAYS);
        List<ViolationRecord> recentViolations = violationRecordRepository.findByUserIdSince(userId, since);

        if (recentViolations.isEmpty()) {
            return false;
        }

        for (ViolationRecord violation : recentViolations) {
            if (violation.getVideoId() != null) {
                Video pastVideo = videoRepository.findById(violation.getVideoId()).orElse(null);
                if (pastVideo != null) {
                    double similarity = calculateContentSimilarity(
                            title, description,
                            pastVideo.getTitle(), pastVideo.getDescription()
                    );
                    if (similarity > 0.7) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double calculateContentSimilarity(String title1, String desc1, String title2, String desc2) {
        String content1 = (title1 == null ? "" : title1) + " " + (desc1 == null ? "" : desc1);
        String content2 = (title2 == null ? "" : title2) + " " + (desc2 == null ? "" : desc2);

        if (content1.isBlank() && content2.isBlank()) return 1.0;
        if (content1.isBlank() || content2.isBlank()) return 0.0;

        String[] words1 = content1.toLowerCase().split("[\\s,，。.!！?？、]+");
        String[] words2 = content2.toLowerCase().split("[\\s,，。.!！?？、]+");

        int matches = 0;
        for (String w1 : words1) {
            if (w1.length() < 2) continue;
            for (String w2 : words2) {
                if (w1.equals(w2)) {
                    matches++;
                    break;
                }
            }
        }

        int totalUnique = (int) (java.util.Arrays.stream(words1)
                .filter(w -> w.length() >= 2)
                .distinct().count()
                + java.util.Arrays.stream(words2)
                .filter(w -> w.length() >= 2)
                .distinct().count());

        return totalUnique > 0 ? (2.0 * matches) / totalUnique : 0.0;
    }

    private void updateUserPenaltyStatus(User user, int addedPoints) {
        int currentPoints = user.getActivePenaltyPoints() != null ? user.getActivePenaltyPoints() : 0;
        int newPoints = currentPoints + addedPoints;

        user.setActivePenaltyPoints(newPoints);
        user.setTotalViolationCount(
                (user.getTotalViolationCount() != null ? user.getTotalViolationCount() : 0) + 1
        );
        user.setLastViolationAt(LocalDateTime.now());

        String newLevel = calculatePenaltyLevel(newPoints);
        user.setPenaltyLevel(newLevel);

        applyPenaltyEffects(user, newLevel);

        LocalDateTime currentExpiry = user.getPenaltyExpiresAt();
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(PENALTY_EXPIRY_DAYS);
        if (currentExpiry == null || newExpiry.isAfter(currentExpiry)) {
            user.setPenaltyExpiresAt(newExpiry);
        }

        userRepository.save(user);
    }

    private String calculatePenaltyLevel(int activePoints) {
        if (activePoints >= SEVERE_LEVEL_THRESHOLD) {
            return "severe";
        } else if (activePoints >= STRICT_LEVEL_THRESHOLD) {
            return "strict";
        } else if (activePoints >= WATCH_LEVEL_THRESHOLD) {
            return "watch";
        } else {
            return "normal";
        }
    }

    private void applyPenaltyEffects(User user, String penaltyLevel) {
        switch (penaltyLevel) {
            case "normal" -> {
                user.setAuditPriority("normal");
                user.setContentVisibility("public");
            }
            case "watch" -> {
                user.setAuditPriority("high");
                user.setContentVisibility("public");
            }
            case "strict" -> {
                user.setAuditPriority("high");
                user.setContentVisibility("followers_only");
            }
            case "severe" -> {
                user.setAuditPriority("highest");
                user.setContentVisibility("private");
            }
            default -> {
                user.setAuditPriority("normal");
                user.setContentVisibility("public");
            }
        }
    }

    public void applyPenaltyToNewVideo(Video video, User user) {
        if (user.getAuditPriority() != null) {
            video.setAuditPriority(user.getAuditPriority());
        }
        if (user.getContentVisibility() != null && "approved".equals(video.getStatus())) {
            video.setVisibility(user.getContentVisibility());
        }
    }

    public void refreshPenaltyStatus(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Integer activePoints = violationRecordRepository.sumActivePenaltyPoints(userId, now);
        int points = activePoints != null ? activePoints : 0;

        user.setActivePenaltyPoints(points);

        String newLevel = calculatePenaltyLevel(points);
        String oldLevel = user.getPenaltyLevel();

        if (!newLevel.equals(oldLevel)) {
            user.setPenaltyLevel(newLevel);
            applyPenaltyEffects(user, newLevel);

            if ("normal".equals(newLevel) && points == 0) {
                user.setPenaltyExpiresAt(null);
            }
        }

        userRepository.save(user);
    }

    public User.PenaltySummary getPenaltySummary(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        refreshPenaltyStatus(userId);
        user = userRepository.findById(userId).orElse(null);

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);

        Long weekViolations = violationRecordRepository.countByUserIdSince(userId, weekAgo);
        Long monthViolations = violationRecordRepository.countByUserIdSince(userId, monthAgo);

        List<Object[]> violationTypes = violationRecordRepository.countViolationTypesByUserIdSince(userId, monthAgo);

        return new User.PenaltySummary(
                user.getPenaltyLevel(),
                user.getActivePenaltyPoints(),
                user.getTotalViolationCount(),
                user.getAuditPriority(),
                user.getContentVisibility(),
                user.getPenaltyExpiresAt(),
                weekViolations != null ? weekViolations.intValue() : 0,
                monthViolations != null ? monthViolations.intValue() : 0,
                violationTypes
        );
    }

    public Page<ViolationRecord> getUserViolationRecords(Long userId, Pageable pageable) {
        return violationRecordRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public boolean manuallyAdjustPenalty(Long userId, int pointAdjustment, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        int currentPoints = user.getActivePenaltyPoints() != null ? user.getActivePenaltyPoints() : 0;
        int newPoints = Math.max(0, currentPoints + pointAdjustment);

        ViolationRecord record = ViolationRecord.builder()
                .userId(userId)
                .violationType("manual_adjustment")
                .violationReason(reason)
                .severity(pointAdjustment > 0 ? "moderate" : "trivial")
                .isRepeatOffense(false)
                .penaltyPoints(pointAdjustment)
                .build();
        violationRecordRepository.save(record);

        user.setActivePenaltyPoints(newPoints);

        String newLevel = calculatePenaltyLevel(newPoints);
        user.setPenaltyLevel(newLevel);
        applyPenaltyEffects(user, newLevel);

        if (newPoints > 0) {
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(PENALTY_EXPIRY_DAYS);
            user.setPenaltyExpiresAt(newExpiry);
        } else {
            user.setPenaltyExpiresAt(null);
        }

        userRepository.save(user);
        return true;
    }

    @Transactional
    public void resetPenalty(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        user.setActivePenaltyPoints(0);
        user.setPenaltyLevel("normal");
        user.setAuditPriority("normal");
        user.setContentVisibility("public");
        user.setPenaltyExpiresAt(null);

        userRepository.save(user);
    }

    public String getAuditPriorityForVideo(Video video) {
        if (video.getAuditPriority() != null && !video.getAuditPriority().equals("normal")) {
            return video.getAuditPriority();
        }

        User user = userRepository.findById(video.getUserId()).orElse(null);
        if (user != null && user.getAuditPriority() != null) {
            return user.getAuditPriority();
        }

        return "normal";
    }
}
