package com.example.shortvideo.repository;

import com.example.shortvideo.entity.Tag;
import com.example.shortvideo.entity.TagSynonym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagSynonymRepository extends JpaRepository<TagSynonym, Long> {
    
    Optional<TagSynonym> findBySynonymTagId(Long synonymTagId);
    
    List<TagSynonym> findByCanonicalTagId(Long canonicalTagId);
    
    boolean existsBySynonymTagId(Long synonymTagId);
    
    void deleteBySynonymTagId(Long synonymTagId);
    
    void deleteByCanonicalTagId(Long canonicalTagId);
    
    @Query("SELECT ts FROM TagSynonym ts " +
           "JOIN Tag t ON ts.synonymTagId = t.id " +
           "WHERE t.name = :synonymName")
    Optional<TagSynonym> findBySynonymTagName(String synonymName);
    
    @Query("SELECT t FROM Tag t " +
           "JOIN TagSynonym ts ON t.id = ts.canonicalTagId " +
           "WHERE ts.synonymTagId = :synonymTagId")
    Optional<Tag> findCanonicalTagBySynonymTagId(Long synonymTagId);
}
