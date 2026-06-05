package com.example.shortvideo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "video_tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "video_id", nullable = false)
    private Long videoId;
    
    @Column(name = "tag_id", nullable = false)
    private Long tagId;
}
