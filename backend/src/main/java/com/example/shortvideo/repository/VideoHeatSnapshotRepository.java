package com.example.shortvideo.repository;

import com.example.shortvideo.entity.VideoHeatSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoHeatSnapshotRepository extends JpaRepository<VideoHeatSnapshot, Long> {

    List<VideoHeatSnapshot> findBySnapshotTimeOrderByRankAsc(LocalDateTime snapshotTime);

    @Query("SELECT s FROM VideoHeatSnapshot s WHERE s.snapshotTime = :snapshotTime ORDER BY s.rank ASC")
    List<VideoHeatSnapshot> findBySnapshotTimeWithLimit(@Param("snapshotTime") LocalDateTime snapshotTime,
                                                        org.springframework.data.domain.Pageable pageable);

    @Query("SELECT MAX(s.snapshotTime) FROM VideoHeatSnapshot s")
    Optional<LocalDateTime> findLatestSnapshotTime();

    @Query("SELECT s FROM VideoHeatSnapshot s WHERE s.snapshotTime = " +
           "(SELECT MAX(s2.snapshotTime) FROM VideoHeatSnapshot s2) " +
           "ORDER BY s.rank ASC")
    List<VideoHeatSnapshot> findLatestSnapshots(org.springframework.data.domain.Pageable pageable);

    boolean existsBySnapshotTime(LocalDateTime snapshotTime);

    void deleteBySnapshotTime(LocalDateTime snapshotTime);

    @Query("SELECT s.snapshotTime FROM VideoHeatSnapshot s GROUP BY s.snapshotTime ORDER BY s.snapshotTime DESC")
    List<LocalDateTime> findAvailableSnapshotTimes(org.springframework.data.domain.Pageable pageable);
}
