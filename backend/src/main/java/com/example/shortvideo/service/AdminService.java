package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.entity.VideoTag;
import com.example.shortvideo.entity.User;
import com.example.shortvideo.repository.TagRepository;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoRepository;
import com.example.shortvideo.repository.VideoTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {
    
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final ContentGovernanceService contentGovernanceService;
    
    public AdminService(VideoRepository videoRepository,
                        UserRepository userRepository,
                        TagRepository tagRepository,
                        VideoTagRepository videoTagRepository,
                        ContentGovernanceService contentGovernanceService) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.videoTagRepository = videoTagRepository;
        this.contentGovernanceService = contentGovernanceService;
    }
    
    public Page<VideoDTO> getPendingVideos(Pageable pageable) {
        return videoRepository.findByStatusWithPagination("pending", pageable)
                .map(this::convertToDTO);
    }

    public Page<VideoDTO> getPendingVideosByPriority(Pageable pageable) {
        return videoRepository.findPendingOrderedByPriority(pageable)
                .map(this::convertToDTO);
    }
    
    public Page<VideoDTO> getVideosByStatus(String status, Pageable pageable) {
        return videoRepository.findByStatusWithPagination(status, pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public boolean updateVideoStatus(Long id, String status) {
        return updateVideoStatusWithReason(id, status, null, null);
    }

    @Transactional
    public boolean updateVideoStatusWithReason(Long id, String status, String violationType, String rejectReason) {
        Video video = videoRepository.findById(id).orElse(null);
        if (video == null) {
            return false;
        }

        String oldStatus = video.getStatus();
        video.setStatus(status);

        if ("rejected".equals(status) && !"rejected".equals(oldStatus)) {
            String type = violationType != null ? violationType : "policy_violation";
            String reason = rejectReason != null ? rejectReason : "内容不符合社区规范";

            boolean isSimilar = contentGovernanceService.checkSimilarContentViolation(
                    video.getUserId(), video.getTitle(), video.getDescription());

            String severity = isSimilar ? "moderate" : "minor";

            video.setRejectReason(reason);
            video.setViolationType(type);

            contentGovernanceService.recordViolation(
                    video.getUserId(), id, type, reason, severity);
        }

        if ("approved".equals(status)) {
            User user = userRepository.findById(video.getUserId()).orElse(null);
            if (user != null) {
                video.setVisibility(user.getContentVisibility() != null ? user.getContentVisibility() : "public");
            }
        }

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

    public Page<VideoDTO> getVideosByAuditPriority(String status, String priority, Pageable pageable) {
        return videoRepository.findByStatusAndAuditPriority(status, priority, pageable)
                .map(this::convertToDTO);
    }
}
