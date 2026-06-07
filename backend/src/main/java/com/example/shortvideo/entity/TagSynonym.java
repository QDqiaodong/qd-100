package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tag_synonyms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagSynonym {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "canonical_tag_id", nullable = false)
    private Long canonicalTagId;
    
    @Column(name = "synonym_tag_id", nullable = false, unique = true)
    private Long synonymTagId;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
