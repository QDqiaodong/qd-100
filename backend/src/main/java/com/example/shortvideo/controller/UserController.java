package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.service.UserService;
import com.example.shortvideo.service.VideoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final VideoService videoService;
    
    public UserController(UserService userService, VideoService videoService) {
        this.userService = userService;
        this.videoService = videoService;
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
    
    @GetMapping("/{userId}/favorites")
    public ApiResponse<List<VideoDTO>> getUserFavorites(@PathVariable Long userId) {
        List<VideoDTO> videos = videoService.getUserFavoriteVideos(userId);
        return ApiResponse.success(videos);
    }
}
