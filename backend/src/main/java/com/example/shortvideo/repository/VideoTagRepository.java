package com.example.shortvideo.repository;

import com.example.shortvideo.entity.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoTagRepository extends JpaRepository<VideoTag, Long> {
    List<VideoTag> findByVideoId(Long videoId);
    List<VideoTag> findByTagId(Long tagId);
    void deleteByVideoId(Long videoId);
}
