package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.entity.Tag;
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
import java.util.stream.Collectors;

@Service
public class VideoService {
    
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final RedisService redisService;
    
    public VideoService(VideoRepository videoRepository, 
                       UserRepository userRepository,
                       TagRepository tagRepository,
                       VideoTagRepository videoTagRepository,
                       RedisService redisService) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.videoTagRepository = videoTagRepository;
        this.redisService = redisService;
    }
    
    public Page<VideoDTO> getVideos(String sort, Pageable pageable) {
        Page<Video> videoPage;
        
        if ("hot".equals(sort)) {
            videoPage = videoRepository.findHotVideos(pageable);
        } else if ("new".equals(sort)) {
            videoPage = videoRepository.findLatestVideos(pageable);
        } else {
            videoPage = videoRepository.findLatestVideos(pageable);
        }
        
        return videoPage.map(this::convertToDTO);
    }
    
    public VideoDTO getVideoById(Long id) {
        Video video = videoRepository.findById(id).orElse(null);
        if (video == null) {
            return null;
        }
        
        String viewCountKey = "video:view:" + id;
        Long viewCount = redisService.increment(viewCountKey);
        if (viewCount != null) {
            video.setViewCount(viewCount.intValue());
        } else {
            // Redis不可用时，使用数据库中的数据并自增
            video.setViewCount(video.getViewCount() + 1);
        }
        videoRepository.save(video);
        
        return convertToDTO(video);
    }
    
    public Video createVideo(Long userId, String title, String description, String videoUrl, String coverUrl, Integer duration, List<String> tags) {
        Video video = Video.builder()
                .userId(userId)
                .title(title)
                .description(description)
                .videoUrl(videoUrl)
                .coverUrl(coverUrl)
                .duration(duration)
                .status("pending")
                .build();
        
        video = videoRepository.save(video);
        
        for (String tagName : tags) {
            Tag tag = tagRepository.findByName(tagName).orElse(null);
            if (tag == null) {
                tag = Tag.builder().name(tagName).build();
                tag = tagRepository.save(tag);
            }
            
            VideoTag videoTag = VideoTag.builder()
                    .videoId(video.getId())
                    .tagId(tag.getId())
                    .build();
            videoTagRepository.save(videoTag);
        }
        
        return video;
    }
    
    public Video updateVideo(Long id, String title, String description) {
        Video video = videoRepository.findById(id).orElse(null);
        if (video == null) {
            return null;
        }
        
        if (title != null) {
            video.setTitle(title);
        }
        if (description != null) {
            video.setDescription(description);
        }
        
        return videoRepository.save(video);
    }
    
    public void deleteVideo(Long id) {
        videoTagRepository.deleteByVideoId(id);
        videoRepository.deleteById(id);
    }
    
    public List<VideoDTO> getUserVideos(Long userId) {
        List<Video> videos = videoRepository.findByUserId(userId);
        return videos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<VideoDTO> getUserFavoriteVideos(Long userId) {
        List<Video> videos = videoRepository.findFavoriteVideosByUserId(userId);
        return videos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    private VideoDTO convertToDTO(Video video) {
        VideoDTO dto = VideoDTO.fromEntity(video);
        
        userRepository.findById(video.getUserId()).ifPresent(user -> {
            dto.setAuthor(UserDTO.fromEntity(user));
        });
        
        List<VideoTag> videoTags = videoTagRepository.findByVideoId(video.getId());
        List<String> tagNames = new ArrayList<>();
        for (VideoTag vt : videoTags) {
            tagRepository.findById(vt.getTagId()).ifPresent(tag -> {
                tagNames.add(tag.getName());
            });
        }
        dto.setTags(tagNames);
        
        return dto;
    }
}
