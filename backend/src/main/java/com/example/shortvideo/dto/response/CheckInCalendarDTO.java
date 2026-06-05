package com.example.shortvideo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInCalendarDTO {
    
    private String yearMonth;
    
    private int totalDays;
    
    private int checkInDays;
    
    private int currentStreak;
    
    private int longestStreak;
    
    private List<DayInfo> days;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayInfo {
        private String date;
        private int dayOfMonth;
        private boolean hasVideo;
        private int videoCount;
        private boolean isStreakBroken;
        private boolean isMostActive;
    }
}
