package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.TagWithSynonymsDTO;
import com.example.shortvideo.entity.Tag;
import com.example.shortvideo.entity.TagSynonym;
import com.example.shortvideo.repository.TagRepository;
import com.example.shortvideo.repository.TagSynonymRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {
    
    private final TagRepository tagRepository;
    private final TagSynonymRepository tagSynonymRepository;
    
    public TagService(TagRepository tagRepository, TagSynonymRepository tagSynonymRepository) {
        this.tagRepository = tagRepository;
        this.tagSynonymRepository = tagSynonymRepository;
    }
    
    public Tag getOrCreateTag(String tagName) {
        return tagRepository.findByName(tagName).orElseGet(() -> {
            Tag tag = Tag.builder()
                    .name(tagName)
                    .isCanonical(true)
                    .build();
            return tagRepository.save(tag);
        });
    }
    
    public Tag getCanonicalTag(String tagName) {
        Tag tag = tagRepository.findByName(tagName).orElse(null);
        if (tag == null) {
            return null;
        }
        
        if (tag.getIsCanonical()) {
            return tag;
        }
        
        return tagSynonymRepository.findCanonicalTagBySynonymTagId(tag.getId()).orElse(tag);
    }
    
    public Tag getCanonicalTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId).orElse(null);
        if (tag == null) {
            return null;
        }
        
        if (tag.getIsCanonical()) {
            return tag;
        }
        
        return tagSynonymRepository.findCanonicalTagBySynonymTagId(tagId).orElse(tag);
    }
    
    public List<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> normalizedTags = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            Tag canonicalTag = getCanonicalTag(tagName);
            if (canonicalTag != null) {
                normalizedTags.add(canonicalTag.getName());
            } else {
                normalizedTags.add(tagName);
            }
        }
        
        return new ArrayList<>(normalizedTags);
    }
    
    public List<Tag> normalizeTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, Tag> tagMap = new LinkedHashMap<>();
        for (String tagName : tagNames) {
            Tag canonicalTag = getCanonicalTag(tagName);
            if (canonicalTag != null) {
                tagMap.put(canonicalTag.getName(), canonicalTag);
            } else {
                Tag newTag = getOrCreateTag(tagName);
                tagMap.put(newTag.getName(), newTag);
            }
        }
        
        return new ArrayList<>(tagMap.values());
    }
    
    @Transactional
    public TagSynonym addSynonym(Long canonicalTagId, Long synonymTagId) {
        Tag canonicalTag = tagRepository.findById(canonicalTagId).orElse(null);
        if (canonicalTag == null) {
            throw new IllegalArgumentException("主标签不存在");
        }
        if (!canonicalTag.getIsCanonical()) {
            throw new IllegalArgumentException("指定的标签不是主标签");
        }
        
        Tag synonymTag = tagRepository.findById(synonymTagId).orElse(null);
        if (synonymTag == null) {
            throw new IllegalArgumentException("同义词标签不存在");
        }
        
        if (tagSynonymRepository.existsBySynonymTagId(synonymTagId)) {
            throw new IllegalArgumentException("该标签已经是其他标签的同义词");
        }
        
        synonymTag.setIsCanonical(false);
        tagRepository.save(synonymTag);
        
        TagSynonym tagSynonym = TagSynonym.builder()
                .canonicalTagId(canonicalTagId)
                .synonymTagId(synonymTagId)
                .build();
        
        return tagSynonymRepository.save(tagSynonym);
    }
    
    @Transactional
    public TagSynonym addSynonymByName(String canonicalTagName, String synonymName) {
        Tag canonicalTag = getOrCreateTag(canonicalTagName);
        if (!canonicalTag.getIsCanonical()) {
            Tag actualCanonical = getCanonicalTag(canonicalTag.getId());
            canonicalTag = actualCanonical;
        }
        
        Tag synonymTag = getOrCreateTag(synonymName);
        
        if (synonymTag.getIsCanonical()) {
            synonymTag.setIsCanonical(false);
            tagRepository.save(synonymTag);
        } else {
            tagSynonymRepository.findBySynonymTagId(synonymTag.getId())
                    .ifPresent(existing -> tagSynonymRepository.delete(existing));
        }
        
        TagSynonym tagSynonym = TagSynonym.builder()
                .canonicalTagId(canonicalTag.getId())
                .synonymTagId(synonymTag.getId())
                .build();
        
        return tagSynonymRepository.save(tagSynonym);
    }
    
    @Transactional
    public boolean removeSynonym(Long synonymTagId) {
        TagSynonym tagSynonym = tagSynonymRepository.findBySynonymTagId(synonymTagId).orElse(null);
        if (tagSynonym == null) {
            return false;
        }
        
        tagSynonymRepository.delete(tagSynonym);
        
        Tag synonymTag = tagRepository.findById(synonymTagId).orElse(null);
        if (synonymTag != null) {
            synonymTag.setIsCanonical(true);
            tagRepository.save(synonymTag);
        }
        
        return true;
    }
    
    public List<Tag> getAllCanonicalTags() {
        return tagRepository.findByIsCanonicalTrue();
    }
    
    public List<Tag> getSynonymTags(Long canonicalTagId) {
        List<TagSynonym> synonyms = tagSynonymRepository.findByCanonicalTagId(canonicalTagId);
        List<Long> synonymTagIds = synonyms.stream()
                .map(TagSynonym::getSynonymTagId)
                .collect(Collectors.toList());
        
        if (synonymTagIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return tagRepository.findAllById(synonymTagIds);
    }
    
    public TagWithSynonymsDTO getTagWithSynonyms(Long canonicalTagId) {
        Tag canonicalTag = tagRepository.findById(canonicalTagId).orElse(null);
        if (canonicalTag == null) {
            return null;
        }
        
        List<Tag> synonyms = getSynonymTags(canonicalTagId);
        
        return TagWithSynonymsDTO.builder()
                .id(canonicalTag.getId())
                .name(canonicalTag.getName())
                .isCanonical(canonicalTag.getIsCanonical())
                .synonyms(synonyms.stream()
                        .map(tag -> TagWithSynonymsDTO.SynonymTagDTO.builder()
                                .id(tag.getId())
                                .name(tag.getName())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
    
    public List<TagWithSynonymsDTO> getAllTagsWithSynonyms() {
        List<Tag> canonicalTags = getAllCanonicalTags();
        return canonicalTags.stream()
                .map(tag -> getTagWithSynonyms(tag.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    public List<Tag> searchTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return tagRepository.searchByName(keyword.trim());
    }
    
    public List<Tag> getOrCreateTags(List<String> tagNames) {
        List<Tag> tags = new ArrayList<>();
        for (String tagName : tagNames) {
            tags.add(getOrCreateTag(tagName));
        }
        return tags;
    }
    
    @Transactional
    public boolean mergeTags(Long sourceTagId, Long targetTagId) {
        Tag sourceTag = tagRepository.findById(sourceTagId).orElse(null);
        Tag targetTag = tagRepository.findById(targetTagId).orElse(null);
        
        if (sourceTag == null || targetTag == null) {
            return false;
        }
        
        if (sourceTagId.equals(targetTagId)) {
            return false;
        }
        
        addSynonym(targetTagId, sourceTagId);
        
        return true;
    }
}
