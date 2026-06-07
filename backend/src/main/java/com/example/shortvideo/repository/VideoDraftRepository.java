package com.example.shortvideo.repository;

import com.example.shortvideo.entity.VideoDraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoDraftRepository extends JpaRepository<VideoDraft, Long> {

    @Query("SELECT d FROM VideoDraft d WHERE d.userId = :userId AND d.status = 'draft' ORDER BY d.updatedAt DESC")
    Page<VideoDraft> findByUserIdAndStatusDraft(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT d FROM VideoDraft d WHERE d.id = :id AND d.userId = :userId AND d.status = 'draft'")
    Optional<VideoDraft> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COUNT(d) FROM VideoDraft d WHERE d.userId = :userId AND d.status = 'draft'")
    long countByUserIdAndStatusDraft(@Param("userId") Long userId);
}
