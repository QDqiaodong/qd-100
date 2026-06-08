package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.CheckInCalendarDTO;
import com.example.shortvideo.dto.response.MorningReportDTO;
import com.example.shortvideo.dto.response.UserDTO;
import com.example.shortvideo.dto.response.VideoDTO;
import com.example.shortvideo.dto.response.VideoMilestoneDTO;
import com.example.shortvideo.dto.response.WatchProgressDTO;
import com.example.shortvideo.entity.Tag;
import com.example.shortvideo.entity.User;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.entity.VideoMilestone;
import com.example.shortvideo.entity.VideoTag;
import com.example.shortvideo.entity.WatchProgress;
import com.example.shortvideo.repository.TagRepository;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoMilestoneRepository;
import com.example.shortvideo.repository.VideoRepository;
import com.example.shortvideo.repository.VideoTagRepository;
import com.example.shortvideo.repository.WatchProgressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {
    
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final VideoMilestoneRepository videoMilestoneRepository;
    private final RedisService redisService;
    private final WatchProgressRepository watchProgressRepository;
    private final HeatService heatService;
    private final TagService tagService;
    
    public VideoService(VideoRepository videoRepository, 
                       UserRepository userRepository,
                       TagRepository tagRepository,
                       VideoTagRepository videoTagRepository,
                       VideoMilestoneRepository videoMilestoneRepository,
                       RedisService redisService,
                       WatchProgressRepository watchProgressRepository,
                       HeatService heatService,
                       TagService tagService) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.videoTagRepository = videoTagRepository;
        this.videoMilestoneRepository = videoMilestoneRepository;
        this.redisService = redisService;
        this.watchProgressRepository = watchProgressRepository;
        this.heatService = heatService;
        this.tagService = tagService;
    }
    
    public Page<VideoDTO> getVideos(String sort, String tag, Pageable pageable) {
        Page<Video> videoPage;

        List<Long> searchTagIds = new ArrayList<>(tagService.getSearchTagIds(tag));

        if ("hot".equals(sort)) {
            videoPage = searchTagIds.isEmpty()
                    ? videoRepository.findHotVideos(pageable)
                    : videoRepository.findHotVideosByTagIds(searchTagIds, pageable);
        } else if ("new".equals(sort)) {
            videoPage = searchTagIds.isEmpty()
                    ? videoRepository.findLatestVideos(pageable)
                    : videoRepository.findLatestVideosByTagIds(searchTagIds, pageable);
        } else {
            videoPage = searchTagIds.isEmpty()
                    ? videoRepository.findLatestVideos(pageable)
                    : videoRepository.findLatestVideosByTagIds(searchTagIds, pageable);
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
            video.setViewCount(video.getViewCount() + 1);
        }
        
        heatService.updateHeatScore(video);
        
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
        heatService.updateHeatScore(video);
        
        List<Tag> normalizedTags = tagService.normalizeTags(tags);
        for (Tag tag : normalizedTags) {
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

    public List<VideoDTO> getUserAllVideos(Long userId) {
        List<Video> videos = videoRepository.findAllByUserIdIncludeAllStatus(userId);
        return videos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<VideoDTO> getUserFavoriteVideos(Long userId) {
        List<Video> videos = videoRepository.findFavoriteVideosByUserId(userId);
        return videos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    public CheckInCalendarDTO getUserCheckInCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        LocalDateTime startDateTime = firstDay.atStartOfDay();
        LocalDateTime endDateTime = lastDay.plusDays(1).atStartOfDay();
        
        List<Video> monthVideos = videoRepository.findByUserIdAndDateRange(userId, startDateTime, endDateTime);
        
        Map<LocalDate, Integer> dateCountMap = new HashMap<>();
        for (Video video : monthVideos) {
            LocalDate date = video.getCreatedAt().toLocalDate();
            dateCountMap.put(date, dateCountMap.getOrDefault(date, 0) + 1);
        }
        
        int maxVideoCount = dateCountMap.values().stream().max(Integer::compareTo).orElse(0);
        
        List<Video> allVideos = videoRepository.findAllByUserId(userId);
        List<LocalDate> allDates = allVideos.stream()
                .map(v -> v.getCreatedAt().toLocalDate())
                .distinct()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        
        int currentStreak = calculateCurrentStreak(allDates);
        int longestStreak = calculateLongestStreak(allDates);
        
        List<CheckInCalendarDTO.DayInfo> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            int count = dateCountMap.getOrDefault(date, 0);
            
            CheckInCalendarDTO.DayInfo dayInfo = CheckInCalendarDTO.DayInfo.builder()
                    .date(date.toString())
                    .dayOfMonth(day)
                    .hasVideo(count > 0)
                    .videoCount(count)
                    .isStreakBroken(false)
                    .isMostActive(count > 0 && count == maxVideoCount)
                    .build();
            days.add(dayInfo);
        }
        
        markStreakBreaks(days, dateCountMap);
        
        return CheckInCalendarDTO.builder()
                .yearMonth(yearMonth.toString())
                .totalDays(yearMonth.lengthOfMonth())
                .checkInDays((int) days.stream().filter(CheckInCalendarDTO.DayInfo::isHasVideo).count())
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .days(days)
                .build();
    }
    
    public List<VideoDTO> getUserVideosByDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<Video> videos = videoRepository.findByUserIdAndDate(userId, startOfDay, endOfDay);
        return videos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    private int calculateCurrentStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        
        int streak = 0;
        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;
        
        for (LocalDate date : dates) {
            if (date.isEqual(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (date.isBefore(checkDate)) {
                break;
            }
        }
        
        if (streak == 0 && !dates.isEmpty()) {
            checkDate = today.minusDays(1);
            for (LocalDate date : dates) {
                if (date.isEqual(checkDate)) {
                    streak++;
                    checkDate = checkDate.minusDays(1);
                } else if (date.isBefore(checkDate)) {
                    break;
                }
            }
        }
        
        return streak;
    }
    
    private int calculateLongestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        
        List<LocalDate> sortedDates = dates.stream().sorted().collect(Collectors.toList());
        
        int longestStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate prev = sortedDates.get(i - 1);
            LocalDate curr = sortedDates.get(i);
            
            if (curr.minusDays(1).isEqual(prev)) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else if (!curr.isEqual(prev)) {
                currentStreak = 1;
            }
        }
        
        return longestStreak;
    }
    
    private void markStreakBreaks(List<CheckInCalendarDTO.DayInfo> days, Map<LocalDate, Integer> dateCountMap) {
        LocalDate prevVideoDate = null;
        
        for (CheckInCalendarDTO.DayInfo day : days) {
            LocalDate currentDate = LocalDate.parse(day.getDate());
            
            if (day.isHasVideo()) {
                if (prevVideoDate != null && !prevVideoDate.plusDays(1).isEqual(currentDate)) {
                    day.setStreakBroken(true);
                }
                prevVideoDate = currentDate;
            }
        }
    }
    
    private VideoDTO convertToDTO(Video video) {
        VideoDTO dto = VideoDTO.fromEntity(video);
        
        userRepository.findById(video.getUserId()).ifPresent(user -> {
            dto.setAuthor(UserDTO.fromEntity(user));
        });
        
        List<VideoTag> videoTags = videoTagRepository.findByVideoId(video.getId());
        Set<String> tagNames = new LinkedHashSet<>();
        for (VideoTag vt : videoTags) {
            tagRepository.findById(vt.getTagId()).ifPresent(tag -> {
                Tag canonicalTag = tagService.getCanonicalTag(tag.getId());
                if (canonicalTag != null) {
                    tagNames.add(canonicalTag.getName());
                } else {
                    tagNames.add(tag.getName());
                }
            });
        }
        dto.setTags(new ArrayList<>(tagNames));
        
        return dto;
    }
    
    public WatchProgressDTO updateWatchProgress(Long userId, Long videoId, Integer currentTime) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return null;
        }
        
        WatchProgress progress = watchProgressRepository.findByUserIdAndVideoId(userId, videoId).orElse(null);
        
        boolean isCompleted = false;
        if (video.getDuration() != null && video.getDuration() > 0) {
            double progressPercent = (double) currentTime / video.getDuration();
            isCompleted = progressPercent >= 0.95;
        }
        
        if (progress == null) {
            progress = WatchProgress.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .currentTime(currentTime)
                    .isCompleted(isCompleted)
                    .build();
        } else {
            progress.setCurrentTime(currentTime);
            progress.setIsCompleted(isCompleted);
        }
        
        progress = watchProgressRepository.save(progress);
        
        return WatchProgressDTO.fromEntity(progress, convertToDTO(video));
    }
    
    public WatchProgressDTO getWatchProgress(Long userId, Long videoId) {
        WatchProgress progress = watchProgressRepository.findByUserIdAndVideoId(userId, videoId).orElse(null);
        if (progress == null) {
            return null;
        }
        
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return null;
        }
        
        return WatchProgressDTO.fromEntity(progress, convertToDTO(video));
    }
    
    public List<WatchProgressDTO> getContinueWatchingVideos(Long userId) {
        List<WatchProgress> progressList = watchProgressRepository.findContinueWatchingVideos(userId);
        
        return progressList.stream()
                .map(progress -> {
                    Video video = videoRepository.findById(progress.getVideoId()).orElse(null);
                    if (video == null) {
                        return null;
                    }
                    return WatchProgressDTO.fromEntity(progress, convertToDTO(video));
                })
                .filter(Objects::nonNull)
                .limit(10)
                .collect(Collectors.toList());
    }

    public List<VideoMilestoneDTO> getVideoMilestones(Long videoId) {
        List<VideoMilestone> milestones = videoMilestoneRepository.findByVideoIdOrderBySortOrderAscTimestampSecondsAsc(videoId);
        return milestones.stream()
                .map(VideoMilestoneDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public VideoMilestoneDTO createVideoMilestone(Long videoId, String title, String description, Integer timestampSeconds, Integer sortOrder) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return null;
        }

        VideoMilestone milestone = VideoMilestone.builder()
                .videoId(videoId)
                .title(title)
                .description(description)
                .timestampSeconds(timestampSeconds)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();

        milestone = videoMilestoneRepository.save(milestone);
        return VideoMilestoneDTO.fromEntity(milestone);
    }

    public VideoMilestoneDTO updateVideoMilestone(Long milestoneId, String title, String description, Integer timestampSeconds, Integer sortOrder) {
        VideoMilestone milestone = videoMilestoneRepository.findById(milestoneId).orElse(null);
        if (milestone == null) {
            return null;
        }

        if (title != null) {
            milestone.setTitle(title);
        }
        if (description != null) {
            milestone.setDescription(description);
        }
        if (timestampSeconds != null) {
            milestone.setTimestampSeconds(timestampSeconds);
        }
        if (sortOrder != null) {
            milestone.setSortOrder(sortOrder);
        }

        milestone = videoMilestoneRepository.save(milestone);
        return VideoMilestoneDTO.fromEntity(milestone);
    }

    public boolean deleteVideoMilestone(Long milestoneId) {
        if (!videoMilestoneRepository.existsById(milestoneId)) {
            return false;
        }
        videoMilestoneRepository.deleteById(milestoneId);
        return true;
    }

    public MorningReportDTO getMorningReport() {
        LocalDate today = LocalDate.now();
        LocalDateTime weekAgo = today.minusDays(7).atStartOfDay();

        List<MorningReportDTO.HotTagDTO> hotTags = getHotTags();
        List<MorningReportDTO.NewAuthorDTO> newAuthors = getNewAuthors(weekAgo);
        List<MorningReportDTO.TrendingVideoDTO> trendingVideos = getTrendingVideos();

        return MorningReportDTO.builder()
                .hotTags(hotTags)
                .newAuthors(newAuthors)
                .trendingVideos(trendingVideos)
                .reportDate(today.toString())
                .build();
    }

    public List<MorningReportDTO.HotTagDTO> getHotTagsSummary() {
        return getHotTags();
    }

    private List<MorningReportDTO.HotTagDTO> getHotTags() {
        List<Object[]> results = tagRepository.findHotCanonicalTagsWithStats();
        List<MorningReportDTO.HotTagDTO> hotTags = new ArrayList<>();

        int rank = 0;
        for (Object[] result : results) {
            if (rank >= 8) break;
            Long id = ((Number) result[0]).longValue();
            String name = (String) result[1];
            Integer videoCount = ((Number) result[2]).intValue();
            Integer viewCount = result[3] != null ? ((Number) result[3]).intValue() : 0;

            String trend = rank < 3 ? "up" : (rank < 5 ? "stable" : "down");

            hotTags.add(MorningReportDTO.HotTagDTO.builder()
                    .id(id)
                    .name(name)
                    .videoCount(videoCount)
                    .viewCount(viewCount)
                    .trend(trend)
                    .build());
            rank++;
        }

        return hotTags;
    }

    private List<MorningReportDTO.NewAuthorDTO> getNewAuthors(LocalDateTime startTime) {
        Pageable pageable = PageRequest.of(0, 6);
        List<User> users = userRepository.findNewAuthors(startTime, pageable);
        List<MorningReportDTO.NewAuthorDTO> newAuthors = new ArrayList<>();

        for (User user : users) {
            Integer videoCount = userRepository.countVideosByUserId(user.getId());
            newAuthors.add(MorningReportDTO.NewAuthorDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .avatar(user.getAvatar())
                    .bio(user.getBio())
                    .videoCount(videoCount != null ? videoCount : 0)
                    .followers(user.getFollowers())
                    .createdAt(user.getCreatedAt().toString())
                    .build());
        }

        return newAuthors;
    }

    private List<MorningReportDTO.TrendingVideoDTO> getTrendingVideos() {
        Pageable pageable = PageRequest.of(0, 6);
        List<Video> videos = videoRepository.findTrendingVideos(pageable);
        List<MorningReportDTO.TrendingVideoDTO> trendingVideos = new ArrayList<>();

        if (videos.isEmpty()) {
            return trendingVideos;
        }

        double maxHeatScore = 0;
        for (Video video : videos) {
            double score = video.getHeatScore() != null ? video.getHeatScore() : 0.0;
            if (score > maxHeatScore) {
                maxHeatScore = score;
            }
        }

        for (int i = 0; i < videos.size(); i++) {
            Video video = videos.get(i);
            VideoDTO videoDTO = convertToDTO(video);

            double heatScore = video.getHeatScore() != null ? video.getHeatScore() : 0.0;
            int growthRate;
            if (maxHeatScore > 0) {
                growthRate = (int) ((heatScore * 100.0) / maxHeatScore);
            } else {
                growthRate = (int) (100.0 / (i + 1));
            }
            growthRate = Math.max(1, Math.min(100, growthRate));

            trendingVideos.add(MorningReportDTO.TrendingVideoDTO.builder()
                    .id(video.getId())
                    .title(video.getTitle())
                    .coverUrl(video.getCoverUrl())
                    .viewCount(video.getViewCount())
                    .likeCount(video.getLikeCount())
                    .growthRate(growthRate)
                    .author(videoDTO.getAuthor())
                    .tags(videoDTO.getTags())
                    .build());
        }

        return trendingVideos;
    }
}
