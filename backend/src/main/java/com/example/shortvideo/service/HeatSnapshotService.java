package com.example.shortvideo.service;

import com.example.shortvideo.entity.AuthorHeatSnapshot;
import com.example.shortvideo.entity.Tag;
import com.example.shortvideo.entity.TagHeatSnapshot;
import com.example.shortvideo.entity.User;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.entity.VideoHeatSnapshot;
import com.example.shortvideo.repository.AuthorHeatSnapshotRepository;
import com.example.shortvideo.repository.TagHeatSnapshotRepository;
import com.example.shortvideo.repository.TagRepository;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoHeatSnapshotRepository;
import com.example.shortvideo.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HeatSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(HeatSnapshotService.class);

    private static final int DEFAULT_RANK_LIMIT = 100;

    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 5.0;
    private static final double FAVORITE_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 2.0;
    private static final double SHARE_WEIGHT = 10.0;
    private static final double FOLLOWER_WEIGHT = 0.5;

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final TagHeatSnapshotRepository tagHeatSnapshotRepository;
    private final VideoHeatSnapshotRepository videoHeatSnapshotRepository;
    private final AuthorHeatSnapshotRepository authorHeatSnapshotRepository;

    public HeatSnapshotService(VideoRepository videoRepository,
                               UserRepository userRepository,
                               TagRepository tagRepository,
                               TagHeatSnapshotRepository tagHeatSnapshotRepository,
                               VideoHeatSnapshotRepository videoHeatSnapshotRepository,
                               AuthorHeatSnapshotRepository authorHeatSnapshotRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.tagHeatSnapshotRepository = tagHeatSnapshotRepository;
        this.videoHeatSnapshotRepository = videoHeatSnapshotRepository;
        this.authorHeatSnapshotRepository = authorHeatSnapshotRepository;
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void generateHourlySnapshots() {
        LocalDateTime snapshotTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        logger.info("开始生成小时热榜快照，时间点: {}", snapshotTime);
        try {
            generateAllSnapshots(snapshotTime);
            logger.info("小时热榜快照生成完成");
        } catch (Exception e) {
            logger.error("生成小时热榜快照失败", e);
        }
    }

    @Transactional
    public void generateAllSnapshots(LocalDateTime snapshotTime) {
        if (tagHeatSnapshotRepository.existsBySnapshotTime(snapshotTime)
                && videoHeatSnapshotRepository.existsBySnapshotTime(snapshotTime)
                && authorHeatSnapshotRepository.existsBySnapshotTime(snapshotTime)) {
            logger.warn("快照时间 {} 已存在，跳过生成", snapshotTime);
            return;
        }

        generateVideoHeatSnapshots(snapshotTime);
        generateTagHeatSnapshots(snapshotTime);
        generateAuthorHeatSnapshots(snapshotTime);
    }

    @Transactional
    public void generateVideoHeatSnapshots(LocalDateTime snapshotTime) {
        logger.info("开始生成作品热度快照...");

        if (videoHeatSnapshotRepository.existsBySnapshotTime(snapshotTime)) {
            logger.info("作品热度快照已存在，跳过");
            return;
        }

        Pageable pageable = PageRequest.of(0, DEFAULT_RANK_LIMIT);
        List<Video> hotVideos = videoRepository.findTrendingVideos(pageable);

        Map<Long, String> authorNameMap = new HashMap<>();
        for (Video video : hotVideos) {
            if (!authorNameMap.containsKey(video.getUserId())) {
                userRepository.findById(video.getUserId()).ifPresent(user ->
                        authorNameMap.put(user.getId(), user.getUsername()));
            }
        }

        List<VideoHeatSnapshot> snapshots = new ArrayList<>();
        int rank = 1;
        for (Video video : hotVideos) {
            VideoHeatSnapshot snapshot = VideoHeatSnapshot.builder()
                    .snapshotTime(snapshotTime)
                    .videoId(video.getId())
                    .title(video.getTitle())
                    .coverUrl(video.getCoverUrl())
                    .authorId(video.getUserId())
                    .authorName(authorNameMap.getOrDefault(video.getUserId(), ""))
                    .heatScore(video.getHeatScore())
                    .viewCount(video.getViewCount())
                    .likeCount(video.getLikeCount())
                    .favoriteCount(video.getFavoriteCount())
                    .commentCount(video.getCommentCount())
                    .shareCount(video.getShareCount())
                    .rank(rank++)
                    .build();
            snapshots.add(snapshot);
        }

        videoHeatSnapshotRepository.saveAll(snapshots);
        logger.info("作品热度快照生成完成，共 {} 条", snapshots.size());
    }

    @Transactional
    public void generateTagHeatSnapshots(LocalDateTime snapshotTime) {
        logger.info("开始生成标签热度快照...");

        if (tagHeatSnapshotRepository.existsBySnapshotTime(snapshotTime)) {
            logger.info("标签热度快照已存在，跳过");
            return;
        }

        List<Object[]> hotTagsWithStats = tagRepository.findHotTagsWithStats();

        List<TagHeatSnapshot> snapshots = new ArrayList<>();
        int rank = 1;
        for (Object[] row : hotTagsWithStats) {
            Long tagId = ((Number) row[0]).longValue();
            String tagName = (String) row[1];
            Long videoCount = ((Number) row[2]).longValue();
            Long viewCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            Long likeCount = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            Long favoriteCount = row[5] != null ? ((Number) row[5]).longValue() : 0L;

            double heatScore = calculateTagHeatScore(viewCount, likeCount, favoriteCount, videoCount);

            TagHeatSnapshot snapshot = TagHeatSnapshot.builder()
                    .snapshotTime(snapshotTime)
                    .tagId(tagId)
                    .tagName(tagName)
                    .heatScore(heatScore)
                    .videoCount(videoCount.intValue())
                    .viewCount(viewCount)
                    .likeCount(likeCount)
                    .favoriteCount(favoriteCount)
                    .commentCount(0L)
                    .rank(rank++)
                    .build();
            snapshots.add(snapshot);

            if (snapshots.size() >= DEFAULT_RANK_LIMIT) {
                break;
            }
        }

        tagHeatSnapshotRepository.saveAll(snapshots);
        logger.info("标签热度快照生成完成，共 {} 条", snapshots.size());
    }

    private double calculateTagHeatScore(long viewCount, long likeCount, long favoriteCount, long videoCount) {
        double baseScore = viewCount * VIEW_WEIGHT
                + likeCount * LIKE_WEIGHT
                + favoriteCount * FAVORITE_WEIGHT;
        return baseScore * (1 + Math.log10(Math.max(videoCount, 1)) * 0.3);
    }

    @Transactional
    public void generateAuthorHeatSnapshots(LocalDateTime snapshotTime) {
        logger.info("开始生成作者热度快照...");

        if (authorHeatSnapshotRepository.existsBySnapshotTime(snapshotTime)) {
            logger.info("作者热度快照已存在，跳过");
            return;
        }

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        List<Video> approvedVideos = videoRepository.findByStatus("approved", pageable).getContent();

        Map<Long, List<Video>> authorVideoMap = approvedVideos.stream()
                .collect(Collectors.groupingBy(Video::getUserId));

        List<AuthorHeatSnapshot> snapshots = new ArrayList<>();

        for (Map.Entry<Long, List<Video>> entry : authorVideoMap.entrySet()) {
            Long authorId = entry.getKey();
            List<Video> videos = entry.getValue();

            User author = userRepository.findById(authorId).orElse(null);
            if (author == null) {
                continue;
            }

            int videoCount = videos.size();
            long totalViewCount = videos.stream().mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0).sum();
            long totalLikeCount = videos.stream().mapToLong(v -> v.getLikeCount() != null ? v.getLikeCount() : 0).sum();
            long totalFavoriteCount = videos.stream().mapToLong(v -> v.getFavoriteCount() != null ? v.getFavoriteCount() : 0).sum();
            long totalCommentCount = videos.stream().mapToLong(v -> v.getCommentCount() != null ? v.getCommentCount() : 0).sum();
            long totalShareCount = videos.stream().mapToLong(v -> v.getShareCount() != null ? v.getShareCount() : 0).sum();

            double heatScore = calculateAuthorHeatScore(
                    totalViewCount, totalLikeCount, totalFavoriteCount,
                    totalCommentCount, totalShareCount,
                    author.getFollowers(), videoCount);

            AuthorHeatSnapshot snapshot = AuthorHeatSnapshot.builder()
                    .snapshotTime(snapshotTime)
                    .authorId(authorId)
                    .authorName(author.getUsername())
                    .avatar(author.getAvatar())
                    .bio(author.getBio())
                    .heatScore(heatScore)
                    .videoCount(videoCount)
                    .totalViewCount(totalViewCount)
                    .totalLikeCount(totalLikeCount)
                    .totalFavoriteCount(totalFavoriteCount)
                    .followers(author.getFollowers())
                    .rank(0)
                    .build();
            snapshots.add(snapshot);
        }

        snapshots.sort(Comparator.comparingDouble(AuthorHeatSnapshot::getHeatScore).reversed());
        for (int i = 0; i < snapshots.size(); i++) {
            snapshots.get(i).setRank(i + 1);
        }

        if (snapshots.size() > DEFAULT_RANK_LIMIT) {
            snapshots = snapshots.subList(0, DEFAULT_RANK_LIMIT);
        }

        authorHeatSnapshotRepository.saveAll(snapshots);
        logger.info("作者热度快照生成完成，共 {} 条", snapshots.size());
    }

    private double calculateAuthorHeatScore(long totalViewCount, long totalLikeCount, long totalFavoriteCount,
                                            long totalCommentCount, long totalShareCount,
                                            Integer followers, int videoCount) {
        double baseScore = totalViewCount * VIEW_WEIGHT
                + totalLikeCount * LIKE_WEIGHT
                + totalFavoriteCount * FAVORITE_WEIGHT
                + totalCommentCount * COMMENT_WEIGHT
                + totalShareCount * SHARE_WEIGHT;

        int followerCount = followers != null ? followers : 0;
        double followerBoost = 1 + Math.log10(Math.max(followerCount, 1)) * FOLLOWER_WEIGHT;
        double videoCountBoost = 1 + Math.log10(Math.max(videoCount, 1)) * 0.3;

        return baseScore * followerBoost * videoCountBoost;
    }

    public List<TagHeatSnapshot> getLatestTagHeatSnapshots(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tagHeatSnapshotRepository.findLatestSnapshots(pageable);
    }

    public List<VideoHeatSnapshot> getLatestVideoHeatSnapshots(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return videoHeatSnapshotRepository.findLatestSnapshots(pageable);
    }

    public List<AuthorHeatSnapshot> getLatestAuthorHeatSnapshots(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return authorHeatSnapshotRepository.findLatestSnapshots(pageable);
    }

    public List<TagHeatSnapshot> getTagHeatSnapshotsByTime(LocalDateTime snapshotTime, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tagHeatSnapshotRepository.findBySnapshotTimeWithLimit(snapshotTime, pageable);
    }

    public List<VideoHeatSnapshot> getVideoHeatSnapshotsByTime(LocalDateTime snapshotTime, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return videoHeatSnapshotRepository.findBySnapshotTimeWithLimit(snapshotTime, pageable);
    }

    public List<AuthorHeatSnapshot> getAuthorHeatSnapshotsByTime(LocalDateTime snapshotTime, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return authorHeatSnapshotRepository.findBySnapshotTimeWithLimit(snapshotTime, pageable);
    }

    public LocalDateTime getLatestSnapshotTime() {
        return videoHeatSnapshotRepository.findLatestSnapshotTime().orElse(null);
    }

    public List<LocalDateTime> getAvailableSnapshotTimes(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return videoHeatSnapshotRepository.findAvailableSnapshotTimes(pageable);
    }
}
