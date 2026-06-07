package com.example.shortvideo.service;

import com.example.shortvideo.entity.Video;
import com.example.shortvideo.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class HeatService {

    private static final Logger logger = LoggerFactory.getLogger(HeatService.class);

    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 5.0;
    private static final double FAVORITE_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 2.0;
    private static final double SHARE_WEIGHT = 10.0;

    private static final double DECAY_HALF_LIFE_HOURS = 12.0;
    private static final double DECAY_LAMBDA = Math.log(2) / DECAY_HALF_LIFE_HOURS;

    private static final double NEW_CONTENT_BOOST_HOURS = 2.0;
    private static final double NEW_CONTENT_BOOST_FACTOR = 2.0;

    private static final double GRAVITY = 1.5;

    private final VideoRepository videoRepository;

    public HeatService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public double calculateHeatScore(Video video) {
        if (video == null || video.getCreatedAt() == null) {
            return 0.0;
        }

        double baseScore = calculateBaseScore(video);

        long hoursSinceCreation = ChronoUnit.HOURS.between(video.getCreatedAt(), LocalDateTime.now());

        double decayFactor = Math.exp(-DECAY_LAMBDA * hoursSinceCreation);

        double newContentBoost = 1.0;
        if (hoursSinceCreation < NEW_CONTENT_BOOST_HOURS) {
            double remainingRatio = (NEW_CONTENT_BOOST_HOURS - hoursSinceCreation) / NEW_CONTENT_BOOST_HOURS;
            newContentBoost = 1.0 + (NEW_CONTENT_BOOST_FACTOR - 1.0) * remainingRatio;
        }

        double timeDecayDenominator = Math.pow(hoursSinceCreation + 2.0, GRAVITY);

        double heatScore = (baseScore * decayFactor * newContentBoost) / timeDecayDenominator * 1000.0;

        return Math.max(0.0, heatScore);
    }

    private double calculateBaseScore(Video video) {
        double viewScore = (video.getViewCount() != null ? video.getViewCount() : 0) * VIEW_WEIGHT;
        double likeScore = (video.getLikeCount() != null ? video.getLikeCount() : 0) * LIKE_WEIGHT;
        double favoriteScore = (video.getFavoriteCount() != null ? video.getFavoriteCount() : 0) * FAVORITE_WEIGHT;
        double commentScore = (video.getCommentCount() != null ? video.getCommentCount() : 0) * COMMENT_WEIGHT;
        double shareScore = (video.getShareCount() != null ? video.getShareCount() : 0) * SHARE_WEIGHT;

        return viewScore + likeScore + favoriteScore + commentScore + shareScore;
    }

    public Video updateHeatScore(Video video) {
        if (video == null) {
            return null;
        }
        double heatScore = calculateHeatScore(video);
        video.setHeatScore(heatScore);
        video.setLastHeatUpdate(LocalDateTime.now());
        return videoRepository.save(video);
    }

    public void updateHeatScoreById(Long videoId) {
        videoRepository.findById(videoId).ifPresent(this::updateHeatScore);
    }

    @Scheduled(fixedRateString = "${video.heat.update.rate:300000}")
    public void batchUpdateHeatScores() {
        logger.info("开始批量更新视频热度分数...");
        
        int pageSize = 100;
        int page = 0;
        int totalUpdated = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<Video> videoPage = videoRepository.findByStatus("approved", pageable);
            
            if (videoPage.isEmpty()) {
                break;
            }

            List<Video> videos = videoPage.getContent();
            for (Video video : videos) {
                try {
                    double heatScore = calculateHeatScore(video);
                    video.setHeatScore(heatScore);
                    video.setLastHeatUpdate(LocalDateTime.now());
                    totalUpdated++;
                } catch (Exception e) {
                    logger.error("更新视频热度失败, videoId: {}", video.getId(), e);
                }
            }
            
            videoRepository.saveAll(videos);
            page++;

            if (page >= videoPage.getTotalPages()) {
                break;
            }
        }

        logger.info("批量更新视频热度分数完成，共更新 {} 个视频", totalUpdated);
    }
}
