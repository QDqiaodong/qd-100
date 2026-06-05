package com.example.shortvideo.repository;

import com.example.shortvideo.entity.VideoMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoMilestoneRepository extends JpaRepository<VideoMilestone, Long> {

    List<VideoMilestone> findByVideoIdOrderBySortOrderAscTimestampSecondsAsc(Long videoId);

    void deleteByVideoId(Long videoId);
}
