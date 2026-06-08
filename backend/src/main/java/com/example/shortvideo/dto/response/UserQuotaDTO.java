package com.example.shortvideo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotaDTO {
    
    private Integer totalVideoCount;
    private Integer maxVideoCount;
    private Double videoCountPercent;
    
    private Integer todayUploadCount;
    private Integer dailyUploadLimit;
    private Double dailyUploadPercent;
    
    private Long usedStorageBytes;
    private Long maxStorageBytes;
    private Double storagePercent;
    
    private Boolean isVideoCountNearLimit;
    private Boolean isDailyUploadNearLimit;
    private Boolean isStorageNearLimit;
    
    private String videoCountStatus;
    private String dailyUploadStatus;
    private String storageStatus;
}
