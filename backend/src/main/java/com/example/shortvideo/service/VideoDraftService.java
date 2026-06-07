package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.VideoDraftDTO;
import com.example.shortvideo.entity.Tag;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.entity.VideoDraft;
import com.example.shortvideo.entity.VideoTag;
import com.example.shortvideo.repository.TagRepository;
import com.example.shortvideo.repository.VideoDraftRepository;
import com.example.shortvideo.repository.VideoRepository;
import com.example.shortvideo.repository.VideoTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoDraftService {

    private final VideoDraftRepository videoDraftRepository;
    private final VideoRepository videoRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final MinIOService minIOService;

    public VideoDraftService(VideoDraftRepository videoDraftRepository,
                             VideoRepository videoRepository,
                             TagRepository tagRepository,
                             VideoTagRepository videoTagRepository,
                             MinIOService minIOService) {
        this.videoDraftRepository = videoDraftRepository;
        this.videoRepository = videoRepository;
        this.tagRepository = tagRepository;
        this.videoTagRepository = videoTagRepository;
        this.minIOService = minIOService;
    }

    public Page<VideoDraftDTO> getDraftList(Long userId, Pageable pageable) {
        Page<VideoDraft> draftPage = videoDraftRepository.findByUserIdAndStatusDraft(userId, pageable);
        return draftPage.map(VideoDraftDTO::fromEntity);
    }

    public VideoDraftDTO getDraft(Long id, Long userId) {
        VideoDraft draft = videoDraftRepository.findByIdAndUserId(id, userId).orElse(null);
        return draft != null ? VideoDraftDTO.fromEntity(draft) : null;
    }

    public long getDraftCount(Long userId) {
        return videoDraftRepository.countByUserIdAndStatusDraft(userId);
    }

    public VideoDraftDTO createDraft(Long userId, String title, String description,
                                     List<String> tags, Integer duration) {
        String tagsText = tags != null ? String.join(",", tags) : null;

        VideoDraft draft = VideoDraft.builder()
                .userId(userId)
                .title(title)
                .description(description)
                .tagsText(tagsText)
                .duration(duration != null ? duration : 0)
                .fileStatus("not_uploaded")
                .status("draft")
                .build();

        draft = videoDraftRepository.save(draft);
        return VideoDraftDTO.fromEntity(draft);
    }

    public VideoDraftDTO updateDraft(Long id, Long userId, String title, String description,
                                     List<String> tags, Integer duration) {
        VideoDraft draft = videoDraftRepository.findByIdAndUserId(id, userId).orElse(null);
        if (draft == null) {
            return null;
        }

        if (title != null) {
            draft.setTitle(title);
        }
        if (description != null) {
            draft.setDescription(description);
        }
        if (tags != null) {
            draft.setTagsText(String.join(",", tags));
        }
        if (duration != null) {
            draft.setDuration(duration);
        }

        draft = videoDraftRepository.save(draft);
        return VideoDraftDTO.fromEntity(draft);
    }

    public VideoDraftDTO uploadDraftVideo(Long draftId, Long userId, MultipartFile file) throws Exception {
        VideoDraft draft = videoDraftRepository.findByIdAndUserId(draftId, userId).orElse(null);
        if (draft == null) {
            return null;
        }

        String videoUrl = minIOService.uploadVideo(file);
        draft.setVideoUrl(videoUrl);
        draft.setVideoFileName(file.getOriginalFilename());
        draft.setFileStatus("uploaded");

        draft = videoDraftRepository.save(draft);
        return VideoDraftDTO.fromEntity(draft);
    }

    public boolean deleteDraft(Long id, Long userId) {
        VideoDraft draft = videoDraftRepository.findByIdAndUserId(id, userId).orElse(null);
        if (draft == null) {
            return false;
        }
        draft.setStatus("deleted");
        videoDraftRepository.save(draft);
        return true;
    }

    public Video publishDraft(Long draftId, Long userId) throws Exception {
        VideoDraft draft = videoDraftRepository.findByIdAndUserId(draftId, userId).orElse(null);
        if (draft == null) {
            return null;
        }

        if (draft.getVideoUrl() == null || draft.getVideoUrl().isEmpty()) {
            throw new IllegalStateException("视频文件未上传，无法发布");
        }

        List<String> tagList = draft.getTagsText() != null && !draft.getTagsText().isEmpty()
                ? Arrays.asList(draft.getTagsText().split(","))
                : List.of();

        Video video = Video.builder()
                .userId(userId)
                .title(draft.getTitle() != null ? draft.getTitle() : "未命名视频")
                .description(draft.getDescription())
                .videoUrl(draft.getVideoUrl())
                .coverUrl(draft.getCoverUrl())
                .duration(draft.getDuration() != null ? draft.getDuration() : 0)
                .status("pending")
                .build();

        video = videoRepository.save(video);

        for (String tagName : tagList) {
            String trimmedName = tagName.trim();
            if (trimmedName.isEmpty()) continue;

            Tag tag = tagRepository.findByName(trimmedName).orElse(null);
            if (tag == null) {
                tag = Tag.builder().name(trimmedName).build();
                tag = tagRepository.save(tag);
            }

            VideoTag videoTag = VideoTag.builder()
                    .videoId(video.getId())
                    .tagId(tag.getId())
                    .build();
            videoTagRepository.save(videoTag);
        }

        draft.setStatus("published");
        videoDraftRepository.save(draft);

        return video;
    }

    public VideoDraftDTO saveOrUpdateDraft(Long draftId, Long userId, String title,
                                           String description, List<String> tags,
                                           Integer duration, MultipartFile file,
                                           MultipartFile coverFile) throws Exception {
        VideoDraft draft;
        String tagsText = tags != null ? String.join(",", tags) : null;

        if (draftId != null) {
            draft = videoDraftRepository.findByIdAndUserId(draftId, userId).orElse(null);
            if (draft == null) {
                return null;
            }

            if (title != null) draft.setTitle(title);
            if (description != null) draft.setDescription(description);
            if (tagsText != null) draft.setTagsText(tagsText);
            if (duration != null) draft.setDuration(duration);

            if (file != null && !file.isEmpty()) {
                String videoUrl = minIOService.uploadVideo(file);
                draft.setVideoUrl(videoUrl);
                draft.setVideoFileName(file.getOriginalFilename());
                draft.setFileStatus("uploaded");
            }

            if (coverFile != null && !coverFile.isEmpty()) {
                String coverUrl = minIOService.uploadCover(coverFile);
                draft.setCoverUrl(coverUrl);
            }
        } else {
            VideoDraft.VideoDraftBuilder builder = VideoDraft.builder()
                    .userId(userId)
                    .title(title)
                    .description(description)
                    .tagsText(tagsText)
                    .duration(duration != null ? duration : 0);

            if (file != null && !file.isEmpty()) {
                String videoUrl = minIOService.uploadVideo(file);
                builder.videoUrl(videoUrl)
                        .videoFileName(file.getOriginalFilename())
                        .fileStatus("uploaded");
            } else {
                builder.fileStatus("not_uploaded");
            }

            if (coverFile != null && !coverFile.isEmpty()) {
                String coverUrl = minIOService.uploadCover(coverFile);
                builder.coverUrl(coverUrl);
            }

            draft = builder.status("draft").build();
        }

        draft = videoDraftRepository.save(draft);
        return VideoDraftDTO.fromEntity(draft);
    }
}
