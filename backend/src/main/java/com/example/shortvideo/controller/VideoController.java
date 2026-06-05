package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.CheckInCalendarDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.dto.response.VideoMilestoneDTO;
import com.example.shortvideo.dto.response.WatchProgressDTO;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.service.MinIOService;
import com.example.shortvideo.service.VideoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    
    private final VideoService videoService;
    private final MinIOService minIOService;
    
    public VideoController(VideoService videoService, MinIOService minIOService) {
        this.videoService = videoService;
        this.minIOService = minIOService;
    }
    
    @GetMapping
    public ApiResponse<Page<VideoDTO>> getVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "hot") String sort) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoDTO> videos = videoService.getVideos(sort, pageable);
        return ApiResponse.success(videos);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<VideoDTO> getVideo(@PathVariable Long id) {
        VideoDTO video = videoService.getVideoById(id);
        if (video == null) {
            return ApiResponse.error(404, "视频不存在");
        }
        return ApiResponse.success(video);
    }
    
    @PostMapping
    public ApiResponse<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String[] tags) {
        
        try {
            String videoUrl = minIOService.uploadVideo(file);
            String coverUrl = null;
            
            List<String> tagList = tags != null ? Arrays.asList(tags) : List.of();
            
            Video video = videoService.createVideo(
                    1L, title, description, videoUrl, coverUrl, 60, tagList);
            
            return ApiResponse.success("上传成功", new UploadResult(video.getId(), video.getStatus()));
        } catch (Exception e) {
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ApiResponse<VideoDTO> updateVideo(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        
        Video video = videoService.updateVideo(id, title, description);
        if (video == null) {
            return ApiResponse.error(404, "视频不存在");
        }
        VideoDTO dto = VideoDTO.fromEntity(video);
        return ApiResponse.success(dto);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ApiResponse.success(null);
    }
    
    @GetMapping("/calendar/{userId}")
    public ApiResponse<CheckInCalendarDTO> getCheckInCalendar(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        YearMonth yearMonth;
        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        } else {
            yearMonth = YearMonth.now();
        }
        
        CheckInCalendarDTO calendar = videoService.getUserCheckInCalendar(
                userId, yearMonth.getYear(), yearMonth.getMonthValue());
        return ApiResponse.success(calendar);
    }
    
    @GetMapping("/user/{userId}/date/{date}")
    public ApiResponse<List<VideoDTO>> getUserVideosByDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        
        LocalDate localDate = LocalDate.parse(date);
        List<VideoDTO> videos = videoService.getUserVideosByDate(userId, localDate);
        return ApiResponse.success(videos);
    }
    
    @PostMapping("/{id}/watch-progress")
    public ApiResponse<WatchProgressDTO> updateWatchProgress(
            @PathVariable Long id,
            @RequestBody WatchProgressRequest request) {
        
        WatchProgressDTO progress = videoService.updateWatchProgress(
                request.userId(), id, request.currentTime());
        if (progress == null) {
            return ApiResponse.error(404, "视频不存在");
        }
        return ApiResponse.success(progress);
    }
    
    @GetMapping("/{id}/watch-progress")
    public ApiResponse<WatchProgressDTO> getWatchProgress(
            @PathVariable Long id,
            @RequestParam Long userId) {
        
        WatchProgressDTO progress = videoService.getWatchProgress(userId, id);
        return ApiResponse.success(progress);
    }
    
    @GetMapping("/continue-watching")
    public ApiResponse<List<WatchProgressDTO>> getContinueWatchingVideos(
            @RequestParam Long userId) {
        
        List<WatchProgressDTO> videos = videoService.getContinueWatchingVideos(userId);
        return ApiResponse.success(videos);
    }
    
    public record UploadResult(Long id, String status) {}
    
    public record WatchProgressRequest(Long userId, Integer currentTime) {}

    @GetMapping("/{id}/milestones")
    public ApiResponse<List<VideoMilestoneDTO>> getVideoMilestones(@PathVariable Long id) {
        List<VideoMilestoneDTO> milestones = videoService.getVideoMilestones(id);
        return ApiResponse.success(milestones);
    }

    @PostMapping("/{id}/milestones")
    public ApiResponse<VideoMilestoneDTO> createVideoMilestone(
            @PathVariable Long id,
            @RequestBody MilestoneRequest request) {

        VideoMilestoneDTO milestone = videoService.createVideoMilestone(
                id, request.title(), request.description(),
                request.timestampSeconds(), request.sortOrder());
        if (milestone == null) {
            return ApiResponse.error(404, "视频不存在");
        }
        return ApiResponse.success(milestone);
    }

    @PutMapping("/milestones/{milestoneId}")
    public ApiResponse<VideoMilestoneDTO> updateVideoMilestone(
            @PathVariable Long milestoneId,
            @RequestBody MilestoneRequest request) {

        VideoMilestoneDTO milestone = videoService.updateVideoMilestone(
                milestoneId, request.title(), request.description(),
                request.timestampSeconds(), request.sortOrder());
        if (milestone == null) {
            return ApiResponse.error(404, "关键时刻不存在");
        }
        return ApiResponse.success(milestone);
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ApiResponse<Void> deleteVideoMilestone(@PathVariable Long milestoneId) {
        boolean deleted = videoService.deleteVideoMilestone(milestoneId);
        if (!deleted) {
            return ApiResponse.error(404, "关键时刻不存在");
        }
        return ApiResponse.success(null);
    }

    public record MilestoneRequest(
            String title,
            String description,
            Integer timestampSeconds,
            Integer sortOrder) {}
}
