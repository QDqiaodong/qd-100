package com.example.shortvideo.service;

import com.example.shortvideo.dto.response.TagWithSynonymsDTO;
import com.example.shortvideo.entity.Tag;
import com.example.shortvideo.entity.TagSynonym;
import com.example.shortvideo.repository.TagRepository;
import com.example.shortvideo.repository.TagSynonymRepository;
import com.example.shortvideo.repository.VideoTagRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {
    private static final Map<String, List<String>> BUILTIN_SYNONYM_GROUPS = createBuiltinSynonymGroups();
    private static final Map<String, String> BUILTIN_CANONICAL_BY_ALIAS = createBuiltinCanonicalByAlias();
    private static final Map<String, String> LEGACY_BROKEN_NAMES = createLegacyBrokenNames();

    private final TagRepository tagRepository;
    private final TagSynonymRepository tagSynonymRepository;
    private final VideoTagRepository videoTagRepository;
    
    public TagService(TagRepository tagRepository,
                      TagSynonymRepository tagSynonymRepository,
                      VideoTagRepository videoTagRepository) {
        this.tagRepository = tagRepository;
        this.tagSynonymRepository = tagSynonymRepository;
        this.videoTagRepository = videoTagRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void repairTagCatalog() {
        repairLegacyBrokenNames();

        for (Map.Entry<String, List<String>> entry : BUILTIN_SYNONYM_GROUPS.entrySet()) {
            Tag canonicalTag = getOrCreateCanonicalTag(entry.getKey());
            if (!canonicalTag.isCanonicalTag()) {
                canonicalTag.setIsCanonical(true);
                canonicalTag = tagRepository.save(canonicalTag);
            }

            for (String synonymName : entry.getValue()) {
                if (!canonicalTag.getName().equals(synonymName)) {
                    ensureSynonymLink(canonicalTag, synonymName);
                }
            }
        }
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
        String normalizedName = normalizeTagName(tagName);
        if (normalizedName == null) {
            return null;
        }

        Tag tag = tagRepository.findByName(normalizedName).orElse(null);
        if (tag == null) {
            String builtinCanonicalName = resolveBuiltinCanonicalName(normalizedName);
            if (builtinCanonicalName == null) {
                return null;
            }
            return tagRepository.findByName(builtinCanonicalName).orElse(null);
        }

        if (tag.isCanonicalTag()) {
            String builtinCanonicalName = resolveBuiltinCanonicalName(normalizedName);
            if (builtinCanonicalName != null && !builtinCanonicalName.equals(normalizedName)) {
                return tagRepository.findByName(builtinCanonicalName).orElse(tag);
            }
            return tag;
        }

        return tagSynonymRepository.findCanonicalTagBySynonymTagId(tag.getId()).orElse(tag);
    }
    
    public Tag getCanonicalTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId).orElse(null);
        if (tag == null) {
            return null;
        }
        
        if (tag.isCanonicalTag()) {
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
            Tag canonicalTag = resolveCanonicalTagForWrite(tagName);
            if (canonicalTag != null) {
                tagMap.put(canonicalTag.getName(), canonicalTag);
            }
        }

        return new ArrayList<>(tagMap.values());
    }

    public Set<Long> getSearchTagIds(String tagName) {
        String normalizedName = normalizeTagName(tagName);
        if (normalizedName == null) {
            return Collections.emptySet();
        }

        Tag canonicalTag = getCanonicalTag(normalizedName);
        if (canonicalTag == null) {
            return Collections.emptySet();
        }

        LinkedHashSet<Long> tagIds = new LinkedHashSet<>();
        tagIds.add(canonicalTag.getId());

        Tag directTag = tagRepository.findByName(normalizedName).orElse(null);
        if (directTag != null) {
            tagIds.add(directTag.getId());
        }

        for (TagSynonym synonym : tagSynonymRepository.findByCanonicalTagId(canonicalTag.getId())) {
            tagIds.add(synonym.getSynonymTagId());
        }

        return tagIds;
    }
    
    @Transactional
    public TagSynonym addSynonym(Long canonicalTagId, Long synonymTagId) {
        Tag canonicalTag = tagRepository.findById(canonicalTagId).orElse(null);
        if (canonicalTag == null) {
            throw new IllegalArgumentException("主标签不存在");
        }
        if (!canonicalTag.isCanonicalTag()) {
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
        if (!canonicalTag.isCanonicalTag()) {
            Tag actualCanonical = getCanonicalTag(canonicalTag.getId());
            canonicalTag = actualCanonical;
        }
        
        Tag synonymTag = getOrCreateTag(synonymName);
        
        if (synonymTag.isCanonicalTag()) {
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
                .isCanonical(canonicalTag.isCanonicalTag())
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

    private Tag resolveCanonicalTagForWrite(String tagName) {
        String normalizedName = normalizeTagName(tagName);
        if (normalizedName == null) {
            return null;
        }

        String builtinCanonicalName = resolveBuiltinCanonicalName(normalizedName);
        if (builtinCanonicalName == null || builtinCanonicalName.equals(normalizedName)) {
            Tag existingTag = tagRepository.findByName(normalizedName).orElse(null);
            if (existingTag == null) {
                return getOrCreateCanonicalTag(normalizedName);
            }
            if (existingTag.isCanonicalTag()) {
                return existingTag;
            }
            return tagSynonymRepository.findCanonicalTagBySynonymTagId(existingTag.getId()).orElse(existingTag);
        }

        Tag canonicalTag = getOrCreateCanonicalTag(builtinCanonicalName);
        ensureSynonymLink(canonicalTag, normalizedName);
        return canonicalTag;
    }

    private Tag getOrCreateCanonicalTag(String canonicalName) {
        return tagRepository.findByName(canonicalName).map(existing -> {
            if (!existing.isCanonicalTag()) {
                existing.setIsCanonical(true);
                return tagRepository.save(existing);
            }
            return existing;
        }).orElseGet(() -> tagRepository.save(Tag.builder()
                .name(canonicalName)
                .isCanonical(true)
                .build()));
    }

    private void ensureSynonymLink(Tag canonicalTag, String synonymName) {
        Tag synonymTag = tagRepository.findByName(synonymName).orElseGet(() -> tagRepository.save(Tag.builder()
                .name(synonymName)
                .isCanonical(false)
                .build()));

        if (Objects.equals(canonicalTag.getId(), synonymTag.getId())) {
            return;
        }

        if (synonymTag.isCanonicalTag()) {
            synonymTag.setIsCanonical(false);
            tagRepository.save(synonymTag);
        }

        TagSynonym existing = tagSynonymRepository.findBySynonymTagId(synonymTag.getId()).orElse(null);
        if (existing == null) {
            tagSynonymRepository.save(TagSynonym.builder()
                    .canonicalTagId(canonicalTag.getId())
                    .synonymTagId(synonymTag.getId())
                    .build());
            return;
        }

        if (!Objects.equals(existing.getCanonicalTagId(), canonicalTag.getId())) {
            existing.setCanonicalTagId(canonicalTag.getId());
            tagSynonymRepository.save(existing);
        }
    }

    private void repairLegacyBrokenNames() {
        for (Map.Entry<String, String> entry : LEGACY_BROKEN_NAMES.entrySet()) {
            Tag brokenTag = tagRepository.findByName(entry.getKey()).orElse(null);
            if (brokenTag == null) {
                continue;
            }

            Tag correctTag = tagRepository.findByName(entry.getValue()).orElse(null);
            if (correctTag == null) {
                brokenTag.setName(entry.getValue());
                brokenTag.setIsCanonical(true);
                tagRepository.save(brokenTag);
                continue;
            }

            if (!Objects.equals(brokenTag.getId(), correctTag.getId())) {
                migrateTagReferences(brokenTag, correctTag);
            }
        }
    }

    private void migrateTagReferences(Tag sourceTag, Tag targetTag) {
        for (var videoTag : videoTagRepository.findByTagId(sourceTag.getId())) {
            if (videoTagRepository.existsByVideoIdAndTagId(videoTag.getVideoId(), targetTag.getId())) {
                videoTagRepository.delete(videoTag);
                continue;
            }
            videoTag.setTagId(targetTag.getId());
            videoTagRepository.save(videoTag);
        }

        for (TagSynonym synonym : tagSynonymRepository.findByCanonicalTagId(sourceTag.getId())) {
            if (Objects.equals(synonym.getSynonymTagId(), targetTag.getId())) {
                tagSynonymRepository.delete(synonym);
                continue;
            }
            synonym.setCanonicalTagId(targetTag.getId());
            tagSynonymRepository.save(synonym);
        }

        tagSynonymRepository.findBySynonymTagId(sourceTag.getId()).ifPresent(tagSynonymRepository::delete);
        tagRepository.delete(sourceTag);
    }

    private String normalizeTagName(String tagName) {
        if (tagName == null) {
            return null;
        }
        String normalizedName = tagName.trim();
        return normalizedName.isEmpty() ? null : normalizedName;
    }

    private String resolveBuiltinCanonicalName(String tagName) {
        return BUILTIN_CANONICAL_BY_ALIAS.get(normalizeTagName(tagName));
    }

    private static Map<String, List<String>> createBuiltinSynonymGroups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("美食", List.of("美食", "美食打卡"));
        groups.put("旅行", List.of("旅行", "旅行日记", "旅游", "风景"));
        groups.put("健身", List.of("健身", "健身打卡", "跑步", "夜跑", "跑步打卡", "晨跑", "瑜伽", "减脂餐", "健身餐"));
        groups.put("学习", List.of("学习", "读书", "编程", "知识分享", "学习打卡"));
        groups.put("音乐", List.of("音乐", "翻唱", "原创音乐", "唱歌"));
        return groups;
    }

    private static Map<String, String> createBuiltinCanonicalByAlias() {
        Map<String, String> aliasToCanonical = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : BUILTIN_SYNONYM_GROUPS.entrySet()) {
            for (String alias : entry.getValue()) {
                aliasToCanonical.put(alias, entry.getKey());
            }
        }
        return aliasToCanonical;
    }

    private static Map<String, String> createLegacyBrokenNames() {
        Map<String, String> brokenNames = new LinkedHashMap<>();
        brokenNames.put("ç¾Žé£Ÿ", "美食");
        brokenNames.put("æ—…è¡Œ", "旅行");
        brokenNames.put("å¥èº«", "健身");
        brokenNames.put("å­¦ä¹ ", "学习");
        brokenNames.put("éŸ³ä¹", "音乐");
        return brokenNames;
    }
}
