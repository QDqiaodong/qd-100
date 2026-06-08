package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.CheckInCalendarDTO;
import com.example.shortvideo.dto.response.MorningReportDTO;
import com.example.shortvideo.dto.response.UserQuotaDTO;
import com.example.shortvideo.dto.response.VideoAppealDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.dto.response.VideoDraftDTO;
import com.example.shortvideo.dto.response.VideoMilestoneDTO;
import com.example.shortvideo.dto.response.WatchProgressDTO;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.service.MinIOService;
import com.example.shortvideo.service.VideoAppealService;
import com.example.shortvideo.service.VideoDraftService;
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
    private final VideoDraftService videoDraftService;
    private final VideoAppealService videoAppealService;

    public VideoController(VideoService videoService, MinIOService minIOService,
                           VideoDraftService videoDraftService,
                           VideoAppealService videoAppealService) {
        this.videoService = videoService;
        this.minIOService = minIOService;
        this.videoDraftService = videoDraftService;
        this.videoAppealService = videoAppealService;
    }
    
    @GetMapping
    public ApiResponse<Page<VideoDTO>> getVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "hot") String sort) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoDTO> videos = videoService.getVideos(sort, tag, pageable);
        return ApiResponse.success(videos);
    }

    @GetMapping("/hot-tags")
    public ApiResponse<List<MorningReportDTO.HotTagDTO>> getHotTags() {
        return ApiResponse.success(videoService.getHotTagsSummary());
    }
    
    @GetMapping("/quota/{userId}")
    public ApiResponse<UserQuotaDTO> getUserQuota(@PathVariable Long userId) {
        UserQuotaDTO quota = videoService.getUserQuota(userId);
        if (quota == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        return ApiResponse.success(quota);
    }

    @GetMapping("/morning-report")
    public ApiResponse<MorningReportDTO> getMorningReport() {
        MorningReportDTO report = videoService.getMorningReport();
        return ApiResponse.success(report);
    }
    
    @GetMapping("/{id:\\d+}")
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
            long fileSize = file.getSize();
            String quotaMessage = videoService.getUploadQuotaMessage(1L, fileSize);
            if (quotaMessage != null) {
                return ApiResponse.error(403, quotaMessage);
            }
            
            String videoUrl = minIOService.uploadVideo(file);
            String coverUrl = null;
            
            List<String> tagList = tags != null ? Arrays.asList(tags) : List.of();
            
            Video video = videoService.createVideo(
                    1L, title, description, videoUrl, coverUrl, 60, fileSize, tagList);
            
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

    @GetMapping("/watch-history")
    public ApiResponse<Page<WatchProgressDTO>> getWatchHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchProgressDTO> history = videoService.getWatchHistory(userId, pageable);
        return ApiResponse.success(history);
    }

    @GetMapping("/watch-history/completed")
    public ApiResponse<Page<WatchProgressDTO>> getCompletedWatchHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchProgressDTO> history = videoService.getCompletedWatchHistory(userId, pageable);
        return ApiResponse.success(history);
    }

    @GetMapping("/watch-history/count")
    public ApiResponse<Long> getWatchProgressCount(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean completed) {
        
        long count = videoService.getWatchProgressCount(userId, completed);
        return ApiResponse.success(count);
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

    @GetMapping("/drafts")
    public ApiResponse<Page<VideoDraftDTO>> getDrafts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "1") Long userId) {

        Pageable pageable = PageRequest.of(page, size);
        Page<VideoDraftDTO> drafts = videoDraftService.getDraftList(userId, pageable);
        return ApiResponse.success(drafts);
    }

    @GetMapping("/drafts/{id}")
    public ApiResponse<VideoDraftDTO> getDraft(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId) {

        VideoDraftDTO draft = videoDraftService.getDraft(id, userId);
        if (draft == null) {
            return ApiResponse.error(404, "草稿不存在");
        }
        return ApiResponse.success(draft);
    }

    @GetMapping("/drafts/count")
    public ApiResponse<Long> getDraftCount(
            @RequestParam(defaultValue = "1") Long userId) {

        long count = videoDraftService.getDraftCount(userId);
        return ApiResponse.success(count);
    }

    @PostMapping("/drafts")
    public ApiResponse<VideoDraftDTO> createDraft(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false) Integer duration) {

        List<String> tagList = tags != null ? Arrays.asList(tags) : List.of();
        VideoDraftDTO draft = videoDraftService.createDraft(userId, title, description, tagList, duration);
        return ApiResponse.success(draft);
    }

    @PutMapping("/drafts/{id}")
    public ApiResponse<VideoDraftDTO> updateDraft(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false) Integer duration) {

        List<String> tagList = tags != null ? Arrays.asList(tags) : null;
        VideoDraftDTO draft = videoDraftService.updateDraft(id, userId, title, description, tagList, duration);
        if (draft == null) {
            return ApiResponse.error(404, "草稿不存在");
        }
        return ApiResponse.success(draft);
    }

    @PostMapping("/drafts/{id}/upload")
    public ApiResponse<VideoDraftDTO> uploadDraftVideo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1") Long userId) {

        try {
            VideoDraftDTO draft = videoDraftService.uploadDraftVideo(id, userId, file);
            if (draft == null) {
                return ApiResponse.error(404, "草稿不存在");
            }
            return ApiResponse.success("上传成功", draft);
        } catch (Exception e) {
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/drafts/save")
    public ApiResponse<VideoDraftDTO> saveDraft(
            @RequestParam(required = false) Long draftId,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false) Integer duration,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {

        try {
            List<String> tagList = tags != null ? Arrays.asList(tags) : null;
            VideoDraftDTO draft = videoDraftService.saveOrUpdateDraft(
                    draftId, userId, title, description, tagList, duration, file, coverFile);
            if (draft == null) {
                return ApiResponse.error(404, "草稿不存在");
            }
            return ApiResponse.success("保存成功", draft);
        } catch (Exception e) {
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/drafts/{id}")
    public ApiResponse<Void> deleteDraft(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId) {

        boolean deleted = videoDraftService.deleteDraft(id, userId);
        if (!deleted) {
            return ApiResponse.error(404, "草稿不存在");
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/drafts/{id}/publish")
    public ApiResponse<UploadResult> publishDraft(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId) {

        try {
            Video video = videoDraftService.publishDraft(id, userId);
            if (video == null) {
                return ApiResponse.error(404, "草稿不存在");
            }
            return ApiResponse.success("发布成功", new UploadResult(video.getId(), video.getStatus()));
        } catch (IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("发布失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/appeals")
    public ApiResponse<VideoAppealDTO> submitAppeal(
            @PathVariable Long id,
            @RequestBody AppealRequest request) {

        try {
            VideoAppealDTO appeal = videoAppealService.submitAppeal(
                    id, request.userId(), request.appealType(), request.content());
            return ApiResponse.success("申诉提交成功", appeal);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        }
    }

    @GetMapping("/{id}/appeals")
    public ApiResponse<Page<VideoAppealDTO>> getVideoAppeals(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<VideoAppealDTO> appeals = videoAppealService.getAppealsByVideoId(id, pageable);
        return ApiResponse.success(appeals);
    }

    public record AppealRequest(Long userId, String appealType, String content) {}
}
