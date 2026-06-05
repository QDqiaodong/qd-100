package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.repository.VideoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    
    private final VideoRepository videoRepository;
    
    public AdminService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }
    
    public Page<VideoDTO> getPendingVideos(Pageable pageable) {
        return videoRepository.findByStatusWithPagination("pending", pageable)
                .map(VideoDTO::fromEntity);
    }
    
    public Page<VideoDTO> getVideosByStatus(String status, Pageable pageable) {
        return videoRepository.findByStatusWithPagination(status, pageable)
                .map(VideoDTO::fromEntity);
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
}
