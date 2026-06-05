package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.CheckInCalendarDTO;
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
    
    public CheckInCalendarDTO getUserCheckInCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        LocalDateTime startDateTime = firstDay.atStartOfDay();
        LocalDateTime endDateTime = lastDay.plusDays(1).atStartOfDay();
        
        List<Object[]> videoCounts = videoRepository.countVideosByDateAndUserId(userId, startDateTime, endDateTime);
        
        Map<LocalDate, Integer> dateCountMap = new HashMap<>();
        for (Object[] row : videoCounts) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            dateCountMap.put(date, count.intValue());
        }
        
        int maxVideoCount = dateCountMap.values().stream().max(Integer::compareTo).orElse(0);
        
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
        
        List<LocalDate> allDates = videoRepository.findDistinctDatesByUserId(userId);
        int currentStreak = calculateCurrentStreak(allDates);
        int longestStreak = calculateLongestStreak(allDates);
        
        markStreakBreaks(days, dateCountMap, yearMonth);
        
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
        List<Video> videos = videoRepository.findByUserIdAndDate(userId, date);
        return videos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    private int calculateCurrentStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        
        int streak = 0;
        LocalDate today = LocalDate.now();
        
        for (LocalDate date : dates) {
            if (date.isEqual(today) || date.isEqual(today.minusDays(streak + 1))) {
                streak++;
            } else if (date.isBefore(today.minusDays(streak + 1))) {
                break;
            }
        }
        
        return streak;
    }
    
    private int calculateLongestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        
        Collections.sort(dates);
        
        int longestStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).minusDays(1).isEqual(dates.get(i - 1))) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else if (!dates.get(i).isEqual(dates.get(i - 1))) {
                currentStreak = 1;
            }
        }
        
        return longestStreak;
    }
    
    private void markStreakBreaks(List<CheckInCalendarDTO.DayInfo> days, Map<LocalDate, Integer> dateCountMap, YearMonth yearMonth) {
        LocalDate prevDate = null;
        
        for (CheckInCalendarDTO.DayInfo day : days) {
            LocalDate currentDate = LocalDate.parse(day.getDate());
            
            if (prevDate != null && day.isHasVideo()) {
                boolean prevHadVideo = dateCountMap.getOrDefault(prevDate, 0) > 0;
                if (!prevHadVideo && !prevDate.plusDays(1).isEqual(currentDate)) {
                    day.setStreakBroken(true);
                }
            }
            
            prevDate = currentDate;
        }
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
