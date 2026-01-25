package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {
    Optional<Study> findByStudyUid(String studyUid);

    Page<Study> findByModality(String modality, Pageable pageable);
    Page<Study> findByStatus(String status, Pageable pageable);
    Page<Study> findByStatusIn(List<String> statuses, Pageable pageable);

    List<Study> findByPatientId(Long patientId);

    long countByStatus(String status);
    long countByModality(String modality);

    // Add this method for StudyService.getStudies()
    @Query("SELECT s FROM Study s WHERE " +
            "(:patientId IS NULL OR s.patient.patientId = :patientId) AND " +
            "(:modality IS NULL OR s.modality = :modality) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:dateFrom IS NULL OR s.studyDate >= :dateFrom) AND " +
            "(:dateTo IS NULL OR s.studyDate <= :dateTo)")
    Page<Study> findByFilters(@Param("patientId") String patientId,
                              @Param("modality") String modality,
                              @Param("status") String status,
                              @Param("dateFrom") LocalDateTime dateFrom,
                              @Param("dateTo") LocalDateTime dateTo,
                              Pageable pageable);
    @Query("SELECT COUNT(s) FROM Study s WHERE FUNCTION('DATE', s.createdAt) = :date")
    long countByCreatedAtDate(@Param("date") LocalDate date);

    Optional<Study> findTopByPatient_IdOrderByStudyDateDesc(Long patientId);
}