package com.example.shortvideo.repository;

import com.example.shortvideo.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT t.id, t.name, COUNT(vt.videoId) as videoCount, SUM(v.viewCount) as viewCount " +
           "FROM Tag t JOIN VideoTag vt ON t.id = vt.tagId " +
           "JOIN Video v ON vt.videoId = v.id " +
           "WHERE v.status = 'approved' AND v.createdAt >= :startTime " +
           "GROUP BY t.id, t.name " +
           "ORDER BY viewCount DESC")
    List<Object[]> findHotTagsWithStats(@Param("startTime") LocalDateTime startTime);
}
