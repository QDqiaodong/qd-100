package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
    
    public record StatusRequest(String status) {}
}
