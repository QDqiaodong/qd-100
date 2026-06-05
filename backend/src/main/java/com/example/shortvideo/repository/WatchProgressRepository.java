package com.example.shortvideo.repository;

import com.example.shortvideo.entity.WatchProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchProgressRepository extends JpaRepository<WatchProgress, Long> {
    
    Optional<WatchProgress> findByUserIdAndVideoId(Long userId, Long videoId);
    
    @Query("SELECT wp FROM WatchProgress wp WHERE wp.userId = :userId AND wp.isCompleted = false ORDER BY wp.updatedAt DESC")
    List<WatchProgress> findUncompletedByUserIdOrderByUpdatedAtDesc(Long userId);
    
    @Query("SELECT wp FROM WatchProgress wp WHERE wp.userId = :userId AND wp.isCompleted = false ORDER BY wp.updatedAt DESC")
    List<WatchProgress> findContinueWatchingVideos(Long userId);
}
