package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.VideoAppealDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.service.AdminService;
import com.example.shortvideo.service.VideoAppealService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    private final VideoAppealService videoAppealService;
    
    public AdminController(AdminService adminService, VideoAppealService videoAppealService) {
        this.adminService = adminService;
        this.videoAppealService = videoAppealService;
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
    
    @PutMapping("/videos/{id}/status")
    public ApiResponse<Void> updateVideoStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {
        
        boolean success = adminService.updateVideoStatus(id, request.status());
        if (!success) {
            return ApiResponse.error(404, "视频不存在");
        }
        return ApiResponse.success(null);
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
    
    public record StatusRequest(String status) {}
    public record AppealReviewRequest(String reviewResult, String reviewComment) {}
}
