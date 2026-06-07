package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.TagWithSynonymsDTO;
import com.example.shortvideo.entity.Tag;
import com.example.shortvideo.entity.TagSynonym;
import com.example.shortvideo.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    
    private final TagService tagService;
    
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }
    
    @GetMapping
    public ApiResponse<List<TagWithSynonymsDTO>> getAllTags() {
        List<TagWithSynonymsDTO> tags = tagService.getAllTagsWithSynonyms();
        return ApiResponse.success(tags);
    }
    
    @GetMapping("/canonical")
    public ApiResponse<List<Tag>> getCanonicalTags() {
        List<Tag> tags = tagService.getAllCanonicalTags();
        return ApiResponse.success(tags);
    }
    
    @GetMapping("/search")
    public ApiResponse<List<Tag>> searchTags(@RequestParam String keyword) {
        List<Tag> tags = tagService.searchTags(keyword);
        return ApiResponse.success(tags);
    }
    
    @GetMapping("/{id}/synonyms")
    public ApiResponse<TagWithSynonymsDTO> getTagWithSynonyms(@PathVariable Long id) {
        TagWithSynonymsDTO tag = tagService.getTagWithSynonyms(id);
        if (tag == null) {
            return ApiResponse.error(404, "标签不存在");
        }
        return ApiResponse.success(tag);
    }
    
    @GetMapping("/canonicalize")
    public ApiResponse<Tag> getCanonicalTag(@RequestParam String name) {
        Tag tag = tagService.getCanonicalTag(name);
        if (tag == null) {
            return ApiResponse.error(404, "标签不存在");
        }
        return ApiResponse.success(tag);
    }
    
    @PostMapping("/synonyms")
    public ApiResponse<TagSynonym> addSynonym(@RequestBody AddSynonymRequest request) {
        try {
            TagSynonym synonym = tagService.addSynonymByName(
                    request.canonicalName(),
                    request.synonymName()
            );
            return ApiResponse.success(synonym);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    @DeleteMapping("/synonyms/{synonymTagId}")
    public ApiResponse<Void> removeSynonym(@PathVariable Long synonymTagId) {
        boolean removed = tagService.removeSynonym(synonymTagId);
        if (!removed) {
            return ApiResponse.error(404, "同义词关系不存在");
        }
        return ApiResponse.success(null);
    }
    
    @PostMapping("/merge")
    public ApiResponse<Void> mergeTags(@RequestBody MergeTagsRequest request) {
        boolean merged = tagService.mergeTags(request.sourceTagId(), request.targetTagId());
        if (!merged) {
            return ApiResponse.error(400, "标签合并失败");
        }
        return ApiResponse.success(null);
    }
    
    public record AddSynonymRequest(String canonicalName, String synonymName) {}
    
    public record MergeTagsRequest(Long sourceTagId, Long targetTagId) {}
}
