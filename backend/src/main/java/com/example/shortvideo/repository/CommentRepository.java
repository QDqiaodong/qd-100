package com.example.shortvideo.repository;

import com.example.shortvideo.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId);
    int countByVideoId(Long videoId);
}
