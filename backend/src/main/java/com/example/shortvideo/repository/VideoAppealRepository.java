package com.example.shortvideo.repository;

import com.example.shortvideo.entity.VideoAppeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoAppealRepository extends JpaRepository<VideoAppeal, Long> {

    Page<VideoAppeal> findByVideoId(Long videoId, Pageable pageable);

    Page<VideoAppeal> findByUserId(Long userId, Pageable pageable);

    Page<VideoAppeal> findByStatus(String status, Pageable pageable);

    List<VideoAppeal> findByVideoIdOrderByCreatedAtDesc(Long videoId);

    @Query("SELECT a FROM VideoAppeal a WHERE a.videoId = :videoId AND a.status = 'pending'")
    Optional<VideoAppeal> findPendingByVideoId(@Param("videoId") Long videoId);

    long countByStatus(String status);
}
