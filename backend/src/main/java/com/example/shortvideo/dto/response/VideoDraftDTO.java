package com.example.shortvideo.dto.response;

import com.example.shortvideo.entity.VideoDraft;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDraftDTO {
    private Long id;
    private String title;
    private String description;
    private List<String> tags;
    private String coverUrl;
    private String videoUrl;
    private String videoFileName;
    private Integer duration;
    private String fileStatus;
    private String status;
    private String createdAt;
    private String updatedAt;

    public static VideoDraftDTO fromEntity(VideoDraft draft) {
        List<String> tags = Collections.emptyList();
        if (draft.getTagsText() != null && !draft.getTagsText().isEmpty()) {
            tags = Arrays.asList(draft.getTagsText().split(","));
        }

        return VideoDraftDTO.builder()
                .id(draft.getId())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .tags(tags)
                .coverUrl(draft.getCoverUrl())
                .videoUrl(draft.getVideoUrl())
                .videoFileName(draft.getVideoFileName())
                .duration(draft.getDuration())
                .fileStatus(draft.getFileStatus())
                .status(draft.getStatus())
                .createdAt(draft.getCreatedAt() != null ? draft.getCreatedAt().toString() : null)
                .updatedAt(draft.getUpdatedAt() != null ? draft.getUpdatedAt().toString() : null)
                .build();
    }
}
