package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {
    Optional<Instance> findByInstanceUid(String instanceUid);
    boolean existsByInstanceUid(String instanceUid);
    List<Instance> findBySeriesId(Long seriesId);
    List<Instance> findBySeriesSeriesUid(String seriesUid);
}