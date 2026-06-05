package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.CommentDTO;
import com.example.shortvideo.dto.response.FavoriteResponse;
import com.example.shortvideo.dto.response.LikeResponse;
import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.entity.Comment;
import com.example.shortvideo.entity.Favorite;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.repository.CommentRepository;
import com.example.shortvideo.repository.FavoriteRepository;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InteractionService {
    
    private final VideoRepository videoRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    
    public InteractionService(VideoRepository videoRepository,
                            FavoriteRepository favoriteRepository,
                            CommentRepository commentRepository,
                            UserRepository userRepository,
                            RedisService redisService) {
        this.videoRepository = videoRepository;
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.redisService = redisService;
    }
    
    public LikeResponse likeVideo(Long videoId) {
        if (!redisService.isAvailable()) {
            return LikeResponse.builder().liked(false).likeCount(0)
                    .errorCode("REDIS_UNAVAILABLE").build();
        }

        String likeKey = "video:like:" + videoId;
        Long currentUserId = 1L;
        String userLikeKey = "user:like:" + currentUserId + ":" + videoId;
        
        boolean liked = Boolean.TRUE.equals(redisService.exists(userLikeKey));
        
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return LikeResponse.builder().liked(false).likeCount(0).build();
        }
        
        int currentLikeCount = video.getLikeCount();
        
        if (liked) {
            redisService.delete(userLikeKey);
            redisService.increment(likeKey, -1);
            currentLikeCount = Math.max(0, currentLikeCount - 1);
            liked = false;
        } else {
            redisService.set(userLikeKey, "true", 86400);
            redisService.increment(likeKey);
            currentLikeCount = currentLikeCount + 1;
            liked = true;
        }

        if (!redisService.isAvailable()) {
            return LikeResponse.builder()
                    .liked(false)
                    .likeCount(video.getLikeCount())
                    .errorCode("REDIS_UNAVAILABLE")
                    .build();
        }
        
        video.setLikeCount(currentLikeCount);
        videoRepository.save(video);
        
        return LikeResponse.builder()
                .liked(liked)
                .likeCount(currentLikeCount)
                .build();
    }
    
    public FavoriteResponse favoriteVideo(Long videoId) {
        Long currentUserId = 1L;
        
        boolean favorited = favoriteRepository.existsByUserIdAndVideoId(currentUserId, videoId);
        
        if (favorited) {
            favoriteRepository.findByUserIdAndVideoId(currentUserId, videoId).ifPresent(favoriteRepository::delete);
            favorited = false;
        } else {
            Favorite favorite = Favorite.builder()
                    .userId(currentUserId)
                    .videoId(videoId)
                    .build();
            favoriteRepository.save(favorite);
            favorited = true;
        }
        
        int favoriteCount = favoriteRepository.countByVideoId(videoId);
        
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video != null) {
            video.setFavoriteCount(favoriteCount);
            videoRepository.save(video);
        }
        
        return FavoriteResponse.builder()
                .favorited(favorited)
                .favoriteCount(favoriteCount)
                .build();
    }
    
    public List<CommentDTO> getComments(Long videoId) {
        List<Comment> comments = commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId);
        return comments.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    public CommentDTO addComment(Long videoId, String content) {
        Long currentUserId = 1L;
        
        Comment comment = Comment.builder()
                .userId(currentUserId)
                .videoId(videoId)
                .content(content)
                .build();
        
        comment = commentRepository.save(comment);
        return convertToDTO(comment);
    }
    
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = CommentDTO.fromEntity(comment);
        
        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            dto.setAuthor(UserDTO.fromEntity(user));
        });
        
        return dto;
    }
}
