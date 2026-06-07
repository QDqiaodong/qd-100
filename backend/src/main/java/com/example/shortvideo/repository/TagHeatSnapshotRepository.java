package com.example.shortvideo.repository;

import com.example.shortvideo.entity.TagHeatSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagHeatSnapshotRepository extends JpaRepository<TagHeatSnapshot, Long> {

    List<TagHeatSnapshot> findBySnapshotTimeOrderByRankAsc(LocalDateTime snapshotTime);

    @Query("SELECT s FROM TagHeatSnapshot s WHERE s.snapshotTime = :snapshotTime ORDER BY s.rank ASC")
    List<TagHeatSnapshot> findBySnapshotTimeWithLimit(@Param("snapshotTime") LocalDateTime snapshotTime,
                                                      org.springframework.data.domain.Pageable pageable);

    @Query("SELECT MAX(s.snapshotTime) FROM TagHeatSnapshot s")
    Optional<LocalDateTime> findLatestSnapshotTime();

    @Query("SELECT s FROM TagHeatSnapshot s WHERE s.snapshotTime = " +
           "(SELECT MAX(s2.snapshotTime) FROM TagHeatSnapshot s2) " +
           "ORDER BY s.rank ASC")
    List<TagHeatSnapshot> findLatestSnapshots(org.springframework.data.domain.Pageable pageable);

    boolean existsBySnapshotTime(LocalDateTime snapshotTime);

    void deleteBySnapshotTime(LocalDateTime snapshotTime);

    @Query("SELECT s.snapshotTime FROM TagHeatSnapshot s GROUP BY s.snapshotTime ORDER BY s.snapshotTime DESC")
    List<LocalDateTime> findAvailableSnapshotTimes(org.springframework.data.domain.Pageable pageable);
}
