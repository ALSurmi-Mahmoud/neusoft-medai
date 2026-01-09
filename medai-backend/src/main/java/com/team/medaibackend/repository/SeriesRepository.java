package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findBySeriesUid(String seriesUid);
    boolean existsBySeriesUid(String seriesUid);
    List<Series> findByStudyId(Long studyId);
    List<Series> findByStudyStudyUid(String studyUid);
}