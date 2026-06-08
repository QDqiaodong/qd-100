package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.dto.response.VideoAppealDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.service.UserService;
import com.example.shortvideo.service.VideoAppealService;
import com.example.shortvideo.service.VideoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final VideoService videoService;
    private final VideoAppealService videoAppealService;
    
    public UserController(UserService userService, VideoService videoService,
                          VideoAppealService videoAppealService) {
        this.userService = userService;
        this.videoService = videoService;
        this.videoAppealService = videoAppealService;
    }
    
    @GetMapping("/me")
    public ApiResponse<UserDTO> getCurrentUser() {
        UserDTO user = userService.getCurrentUser();
        return ApiResponse.success(user);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<UserDTO> getUser(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        return ApiResponse.success(user);
    }
    
    @GetMapping("/{userId}/videos")
    public ApiResponse<List<VideoDTO>> getUserVideos(@PathVariable Long userId) {
        List<VideoDTO> videos = videoService.getUserVideos(userId);
        return ApiResponse.success(videos);
    }

    @GetMapping("/{userId}/videos/all")
    public ApiResponse<List<VideoDTO>> getUserAllVideos(@PathVariable Long userId) {
        List<VideoDTO> videos = videoService.getUserAllVideos(userId);
        return ApiResponse.success(videos);
    }

    @GetMapping("/{userId}/appeals")
    public ApiResponse<Page<VideoAppealDTO>> getUserAppeals(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoAppealDTO> appeals = videoAppealService.getAppealsByUserId(userId, pageable);
        return ApiResponse.success(appeals);
    }
    
    @GetMapping("/{userId}/favorites")
    public ApiResponse<List<VideoDTO>> getUserFavorites(@PathVariable Long userId) {
        List<VideoDTO> videos = videoService.getUserFavoriteVideos(userId);
        return ApiResponse.success(videos);
    }
}
