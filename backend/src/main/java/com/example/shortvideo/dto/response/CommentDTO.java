package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private Long userId;
    private Long videoId;
    private String content;
    private UserDTO author;
    private String createdAt;
    
    public static CommentDTO fromEntity(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .videoId(comment.getVideoId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt().toString())
                .build();
    }
}
