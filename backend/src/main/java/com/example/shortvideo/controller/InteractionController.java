package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.CommentDTO;
import com.example.shortvideo.dto.response.FavoriteResponse;
import com.example.shortvideo.dto.response.LikeResponse;
import com.example.shortvideo.service.InteractionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos/{videoId}")
public class InteractionController {
    
    private final InteractionService interactionService;
    
    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }
    
    @PostMapping("/like")
    public ApiResponse<LikeResponse> likeVideo(@PathVariable Long videoId) {
        LikeResponse response = interactionService.likeVideo(videoId);
        return ApiResponse.success(response);
    }
    
    @PostMapping("/favorite")
    public ApiResponse<FavoriteResponse> favoriteVideo(@PathVariable Long videoId) {
        FavoriteResponse response = interactionService.favoriteVideo(videoId);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/comments")
    public ApiResponse<List<CommentDTO>> getComments(@PathVariable Long videoId) {
        List<CommentDTO> comments = interactionService.getComments(videoId);
        return ApiResponse.success(comments);
    }
    
    @PostMapping("/comments")
    public ApiResponse<CommentDTO> addComment(
            @PathVariable Long videoId,
            @RequestBody CommentRequest request) {
        
        CommentDTO comment = interactionService.addComment(videoId, request.content());
        return ApiResponse.success(comment);
    }
    
    public record CommentRequest(String content) {}
}
