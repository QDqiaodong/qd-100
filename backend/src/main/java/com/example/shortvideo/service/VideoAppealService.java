package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.dto.response.VideoAppealDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.entity.VideoAppeal;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoAppealRepository;
import com.example.shortvideo.repository.VideoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VideoAppealService {

    private final VideoAppealRepository videoAppealRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public VideoAppealService(VideoAppealRepository videoAppealRepository,
                              VideoRepository videoRepository,
                              UserRepository userRepository) {
        this.videoAppealRepository = videoAppealRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    public VideoAppealDTO submitAppeal(Long videoId, Long userId, String appealType, String content) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            throw new IllegalArgumentException("视频不存在");
        }
        if (!"rejected".equals(video.getStatus())) {
            throw new IllegalStateException("只有被拒绝的视频才能申诉");
        }
        if (!video.getUserId().equals(userId)) {
            throw new SecurityException("只能申诉自己的视频");
        }

        Optional<VideoAppeal> pendingAppeal = videoAppealRepository.findPendingByVideoId(videoId);
        if (pendingAppeal.isPresent()) {
            throw new IllegalStateException("该视频已有待处理的申诉");
        }

        VideoAppeal appeal = VideoAppeal.builder()
                .videoId(videoId)
                .userId(userId)
                .appealType(appealType)
                .content(content)
                .status("pending")
                .build();

        VideoAppeal saved = videoAppealRepository.save(appeal);
        return convertToDTO(saved);
    }

    public Page<VideoAppealDTO> getAppealsByUserId(Long userId, Pageable pageable) {
        return videoAppealRepository.findByUserId(userId, pageable)
                .map(this::convertToDTO);
    }

    public Page<VideoAppealDTO> getAppealsByVideoId(Long videoId, Pageable pageable) {
        return videoAppealRepository.findByVideoId(videoId, pageable)
                .map(this::convertToDTO);
    }

    public Page<VideoAppealDTO> getAllAppeals(Pageable pageable) {
        return videoAppealRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public Page<VideoAppealDTO> getAppealsByStatus(String status, Pageable pageable) {
        return videoAppealRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
    }

    public VideoAppealDTO getAppealById(Long id) {
        VideoAppeal appeal = videoAppealRepository.findById(id).orElse(null);
        if (appeal == null) {
            return null;
        }
        return convertToDTO(appeal);
    }

    public VideoAppealDTO reviewAppeal(Long appealId, Long reviewerId, String reviewResult, String reviewComment) {
        VideoAppeal appeal = videoAppealRepository.findById(appealId).orElse(null);
        if (appeal == null) {
            throw new IllegalArgumentException("申诉不存在");
        }
        if (!"pending".equals(appeal.getStatus())) {
            throw new IllegalStateException("该申诉已处理");
        }

        appeal.setStatus("reviewed");
        appeal.setReviewerId(reviewerId);
        appeal.setReviewResult(reviewResult);
        appeal.setReviewComment(reviewComment);

        if ("upheld".equals(reviewResult)) {
            Video video = videoRepository.findById(appeal.getVideoId()).orElse(null);
            if (video != null) {
                video.setStatus("pending");
                videoRepository.save(video);
            }
        }

        VideoAppeal saved = videoAppealRepository.save(appeal);
        return convertToDTO(saved);
    }

    public long getPendingAppealCount() {
        return videoAppealRepository.countByStatus("pending");
    }

    private VideoAppealDTO convertToDTO(VideoAppeal appeal) {
        VideoAppealDTO dto = VideoAppealDTO.fromEntity(appeal);

        videoRepository.findById(appeal.getVideoId()).ifPresent(video -> {
            dto.setVideo(VideoDTO.fromEntity(video));
        });

        userRepository.findById(appeal.getUserId()).ifPresent(user -> {
            dto.setUser(UserDTO.fromEntity(user));
        });

        return dto;
    }
}
