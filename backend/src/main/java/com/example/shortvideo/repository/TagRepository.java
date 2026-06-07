package com.example.shortvideo.repository;

import com.example.shortvideo.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    boolean existsByName(String name);
    
    @Query("SELECT t FROM Tag t WHERE t.isCanonical = true OR t.isCanonical IS NULL")
    List<Tag> findByIsCanonicalTrue();
    
    @Query("SELECT t FROM Tag t WHERE t.isCanonical = false")
    List<Tag> findByIsCanonicalFalse();
    
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Tag> searchByName(String keyword);

    @Query("SELECT t.id, t.name, COUNT(vt.videoId) as videoCount, " +
           "SUM(v.viewCount) as viewCount, SUM(v.likeCount) as likeCount, SUM(v.favoriteCount) as favoriteCount " +
           "FROM Tag t JOIN VideoTag vt ON t.id = vt.tagId " +
           "JOIN Video v ON vt.videoId = v.id " +
           "WHERE v.status = 'approved' " +
           "GROUP BY t.id, t.name " +
           "ORDER BY (SUM(v.viewCount) + SUM(v.likeCount) * 3 + SUM(v.favoriteCount) * 2) DESC")
    List<Object[]> findHotTagsWithStats();
    
    @Query(value = "SELECT " +
                   "  COALESCE(ts.canonical_tag_id, t.id) AS tag_id, " +
                   "  COALESCE(canonical_tag.name, t.name) AS tag_name, " +
                   "  COUNT(DISTINCT vt.video_id) AS video_count, " +
                   "  COALESCE(SUM(v.view_count), 0) AS view_count, " +
                   "  COALESCE(SUM(v.like_count), 0) AS like_count, " +
                   "  COALESCE(SUM(v.favorite_count), 0) AS favorite_count, " +
                   "  (COALESCE(SUM(v.view_count), 0) + COALESCE(SUM(v.like_count), 0) * 3 + COALESCE(SUM(v.favorite_count), 0) * 2) AS heat_score " +
                   "FROM tags t " +
                   "JOIN video_tags vt ON t.id = vt.tag_id " +
                   "JOIN videos v ON vt.video_id = v.id " +
                   "LEFT JOIN tag_synonyms ts ON t.id = ts.synonym_tag_id " +
                   "LEFT JOIN tags canonical_tag ON ts.canonical_tag_id = canonical_tag.id " +
                   "WHERE v.status = 'approved' " +
                   "GROUP BY COALESCE(ts.canonical_tag_id, t.id), COALESCE(canonical_tag.name, t.name) " +
                   "ORDER BY heat_score DESC",
           nativeQuery = true)
    List<Object[]> findHotCanonicalTagsWithStats();
}
