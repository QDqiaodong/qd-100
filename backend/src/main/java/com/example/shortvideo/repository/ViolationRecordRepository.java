package com.example.shortvideo.repository;

import com.example.shortvideo.entity.ViolationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViolationRecordRepository extends JpaRepository<ViolationRecord, Long> {

    List<ViolationRecord> findByUserId(Long userId);

    Page<ViolationRecord> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT v FROM ViolationRecord v WHERE v.userId = :userId AND v.createdAt >= :since ORDER BY v.createdAt DESC")
    List<ViolationRecord> findByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(v) FROM ViolationRecord v WHERE v.userId = :userId AND v.createdAt >= :since")
    Long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(v) FROM ViolationRecord v WHERE v.userId = :userId AND v.violationType = :violationType AND v.createdAt >= :since")
    Long countByUserIdAndViolationTypeSince(@Param("userId") Long userId,
                                             @Param("violationType") String violationType,
                                             @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(v.penaltyPoints), 0) FROM ViolationRecord v WHERE v.userId = :userId AND (v.expiresAt IS NULL OR v.expiresAt > :now)")
    Integer sumActivePenaltyPoints(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT v.violationType, COUNT(v) as cnt FROM ViolationRecord v WHERE v.userId = :userId AND v.createdAt >= :since GROUP BY v.violationType ORDER BY cnt DESC")
    List<Object[]> countViolationTypesByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT v FROM ViolationRecord v WHERE v.userId = :userId AND v.videoId = :videoId ORDER BY v.createdAt DESC")
    List<ViolationRecord> findByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);
}
