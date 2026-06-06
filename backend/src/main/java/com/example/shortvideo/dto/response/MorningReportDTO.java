package com.example.shortvideo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MorningReportDTO {
    private List<HotTagDTO> hotTags;
    private List<NewAuthorDTO> newAuthors;
    private List<TrendingVideoDTO> trendingVideos;
    private String reportDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotTagDTO {
        private Long id;
        private String name;
        private Integer videoCount;
        private Integer viewCount;
        private String trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewAuthorDTO {
        private Long id;
        private String username;
        private String avatar;
        private String bio;
        private Integer videoCount;
        private Integer followers;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendingVideoDTO {
        private Long id;
        private String title;
        private String coverUrl;
        private Integer viewCount;
        private Integer likeCount;
        private Integer growthRate;
        private UserDTO author;
        private List<String> tags;
    }
}
