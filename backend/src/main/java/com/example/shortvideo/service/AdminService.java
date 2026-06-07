package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.entity.VideoTag;
import com.example.shortvideo.repository.TagRepository;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoRepository;
import com.example.shortvideo.repository.VideoTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {
    
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    
    public AdminService(VideoRepository videoRepository,
                        UserRepository userRepository,
                        TagRepository tagRepository,
                        VideoTagRepository videoTagRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.videoTagRepository = videoTagRepository;
    }
    
    public Page<VideoDTO> getPendingVideos(Pageable pageable) {
        return videoRepository.findByStatusWithPagination("pending", pageable)
                .map(this::convertToDTO);
    }
    
    public Page<VideoDTO> getVideosByStatus(String status, Pageable pageable) {
        return videoRepository.findByStatusWithPagination(status, pageable)
                .map(this::convertToDTO);
    }
    
    public boolean updateVideoStatus(Long id, String status) {
        Video video = videoRepository.findById(id).orElse(null);
        if (video == null) {
            return false;
        }
        
        video.setStatus(status);
        videoRepository.save(video);
        return true;
    }

    private VideoDTO convertToDTO(Video video) {
        VideoDTO dto = VideoDTO.fromEntity(video);

        userRepository.findById(video.getUserId()).ifPresent(user -> {
            dto.setAuthor(UserDTO.fromEntity(user));
        });

        List<VideoTag> videoTags = videoTagRepository.findByVideoId(video.getId());
        List<String> tagNames = new ArrayList<>();
        for (VideoTag videoTag : videoTags) {
            tagRepository.findById(videoTag.getTagId()).ifPresent(tag -> tagNames.add(tag.getName()));
        }
        dto.setTags(tagNames);

        return dto;
    }
}
