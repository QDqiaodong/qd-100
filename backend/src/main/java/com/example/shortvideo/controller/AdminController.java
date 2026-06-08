package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.PenaltyDetailDTO;
import com.example.shortvideo.dto.response.VideoAppealDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.dto.response.ViolationRecordDTO;
import com.example.shortvideo.entity.User;
import com.example.shortvideo.service.AdminService;
import com.example.shortvideo.service.ContentGovernanceService;
import com.example.shortvideo.service.VideoAppealService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    private final VideoAppealService videoAppealService;
    private final ContentGovernanceService contentGovernanceService;
    
    public AdminController(AdminService adminService,
                           VideoAppealService videoAppealService,
                           ContentGovernanceService contentGovernanceService) {
        this.adminService = adminService;
        this.videoAppealService = videoAppealService;
        this.contentGovernanceService = contentGovernanceService;
    }
    
    @GetMapping("/videos")
    public ApiResponse<Page<VideoDTO>> getPendingVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "pending") String status) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoDTO> videos = adminService.getVideosByStatus(status, pageable);
        return ApiResponse.success(videos);
    }

    @GetMapping("/videos/priority")
    public ApiResponse<Page<VideoDTO>> getPendingVideosByPriority(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<VideoDTO> videos = adminService.getPendingVideosByPriority(pageable);
        return ApiResponse.success(videos);
    }

    @GetMapping("/videos/audit-priority/{priority}")
    public ApiResponse<Page<VideoDTO>> getVideosByAuditPriority(
            @PathVariable String priority,
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<VideoDTO> videos = adminService.getVideosByAuditPriority(status, priority, pageable);
        return ApiResponse.success(videos);
    }
    
    @PutMapping("/videos/{id}/status")
    public ApiResponse<Void> updateVideoStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {

        boolean success = adminService.updateVideoStatusWithReason(
                id, request.status(), request.violationType(), request.rejectReason());
        if (!success) {
            return ApiResponse.error(404, "视频不存在");
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/users/{userId}/penalty")
    public ApiResponse<PenaltyDetailDTO> getUserPenaltyDetail(@PathVariable Long userId) {
        User.PenaltySummary summary = contentGovernanceService.getPenaltySummary(userId);
        if (summary == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        List<PenaltyDetailDTO.ViolationTypeStatDTO> typeStats = summary.getViolationTypeStats() != null
                ? summary.getViolationTypeStats().stream()
                .map(arr -> PenaltyDetailDTO.ViolationTypeStatDTO.builder()
                        .violationType((String) arr[0])
                        .count(((Number) arr[1]).longValue())
                        .build())
                .collect(Collectors.toList())
                : null;

        PenaltyDetailDTO dto = PenaltyDetailDTO.builder()
                .penaltyLevel(summary.getPenaltyLevel())
                .activePenaltyPoints(summary.getActivePenaltyPoints())
                .totalViolationCount(summary.getTotalViolationCount())
                .auditPriority(summary.getAuditPriority())
                .contentVisibility(summary.getContentVisibility())
                .penaltyExpiresAt(summary.getPenaltyExpiresAt() != null ? summary.getPenaltyExpiresAt().toString() : null)
                .weekViolationCount(summary.getWeekViolationCount())
                .monthViolationCount(summary.getMonthViolationCount())
                .violationTypeStats(typeStats)
                .build();

        return ApiResponse.success(dto);
    }

    @GetMapping("/users/{userId}/violations")
    public ApiResponse<Page<ViolationRecordDTO>> getUserViolations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ViolationRecordDTO> violations = contentGovernanceService.getUserViolationRecords(userId, pageable)
                .map(ViolationRecordDTO::fromEntity);
        return ApiResponse.success(violations);
    }

    @PostMapping("/users/{userId}/penalty/adjust")
    public ApiResponse<Void> adjustUserPenalty(
            @PathVariable Long userId,
            @RequestBody PenaltyAdjustRequest request) {

        boolean success = contentGovernanceService.manuallyAdjustPenalty(
                userId, request.pointAdjustment(), request.reason());
        if (!success) {
            return ApiResponse.error(404, "用户不存在");
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/users/{userId}/penalty/reset")
    public ApiResponse<Void> resetUserPenalty(@PathVariable Long userId) {
        contentGovernanceService.resetPenalty(userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/users/{userId}/penalty/refresh")
    public ApiResponse<PenaltyDetailDTO> refreshUserPenalty(@PathVariable Long userId) {
        contentGovernanceService.refreshPenaltyStatus(userId);
        return getUserPenaltyDetail(userId);
    }

    @GetMapping("/appeals")
    public ApiResponse<Page<VideoAppealDTO>> getAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size);
        Page<VideoAppealDTO> appeals;
        if (status != null && !status.isEmpty()) {
            appeals = videoAppealService.getAppealsByStatus(status, pageable);
        } else {
            appeals = videoAppealService.getAllAppeals(pageable);
        }
        return ApiResponse.success(appeals);
    }

    @GetMapping("/appeals/stats")
    public ApiResponse<Long> getPendingAppealCount() {
        long count = videoAppealService.getPendingAppealCount();
        return ApiResponse.success(count);
    }

    @GetMapping("/appeals/{id}")
    public ApiResponse<VideoAppealDTO> getAppealDetail(@PathVariable Long id) {
        VideoAppealDTO appeal = videoAppealService.getAppealById(id);
        if (appeal == null) {
            return ApiResponse.error(404, "申诉不存在");
        }
        return ApiResponse.success(appeal);
    }

    @PutMapping("/appeals/{id}/review")
    public ApiResponse<VideoAppealDTO> reviewAppeal(
            @PathVariable Long id,
            @RequestBody AppealReviewRequest request) {

        try {
            VideoAppealDTO appeal = videoAppealService.reviewAppeal(
                    id, 1L, request.reviewResult(), request.reviewComment());
            return ApiResponse.success(appeal);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    public record StatusRequest(String status, String violationType, String rejectReason) {}
    public record AppealReviewRequest(String reviewResult, String reviewComment) {}
    public record PenaltyAdjustRequest(int pointAdjustment, String reason) {}
}
