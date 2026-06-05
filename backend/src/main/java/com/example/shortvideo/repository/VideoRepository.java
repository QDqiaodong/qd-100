package com.example.shortvideo.repository;

import com.example.shortvideo.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.status = 'approved' ORDER BY v.likeCount DESC")
    Page<Video> findHotVideos(Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.status = 'approved' ORDER BY v.createdAt DESC")
    Page<Video> findLatestVideos(Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.userId = :userId AND v.status = 'approved'")
    List<Video> findByUserId(@Param("userId") Long userId);

    @Query("SELECT v FROM Favorite f JOIN Video v ON f.videoId = v.id WHERE f.userId = :userId AND v.status = 'approved' ORDER BY f.createdAt DESC")
    List<Video> findFavoriteVideosByUserId(@Param("userId") Long userId);
    
    @Query("SELECT v FROM Video v WHERE v.status = :status")
    Page<Video> findByStatusWithPagination(@Param("status") String status, Pageable pageable);
}
