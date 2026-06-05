package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.entity.User;
import com.example.shortvideo.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public UserDTO getCurrentUser() {
        User user = userRepository.findById(1L).orElse(null);
        if (user == null) {
            user = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password("password")
                    .bio("这是一个测试用户")
                    .followers(100)
                    .following(50)
                    .build();
            user = userRepository.save(user);
        }
        UserDTO dto = UserDTO.fromEntity(user);
        dto.setVideoCount(5);
        return dto;
    }
    
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return null;
        }
        UserDTO dto = UserDTO.fromEntity(user);
        dto.setVideoCount(5);
        return dto;
    }
    
    public User getUserEntityById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
