package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportUid(String reportUid);

    boolean existsByReportUid(String reportUid);

    List<Report> findByStudyId(Long studyId);

    List<Report> findByStudyStudyUid(String studyUid);

    List<Report> findByAuthorId(Long authorId);

    Page<Report> findByStatus(String status, Pageable pageable);

    Page<Report> findByAuthorId(Long authorId, Pageable pageable);

    @Query("""
        SELECT r FROM Report r
        JOIN r.study s
        JOIN s.patient p
        WHERE p.id = :patientId
        ORDER BY r.createdAt DESC
    """)
    List<Report> findByPatientId(@Param("patientId") Long patientId);
}
