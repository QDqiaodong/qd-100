package com.example.shortvideo.repository;

import com.example.shortvideo.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndVideoId(Long userId, Long videoId);
    boolean existsByUserIdAndVideoId(Long userId, Long videoId);
    int countByVideoId(Long videoId);
}
