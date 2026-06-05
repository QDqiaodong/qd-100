package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private String bio;
    private Integer followers;
    private Integer following;
    private Integer videoCount;
    private String createdAt;
    
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .followers(user.getFollowers())
                .following(user.getFollowing())
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}
