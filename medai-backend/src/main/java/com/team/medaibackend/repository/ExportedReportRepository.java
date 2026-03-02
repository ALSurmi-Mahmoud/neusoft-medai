package com.team.medaibackend.repository;

import com.team.medaibackend.entity.ExportedReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportedReportRepository extends JpaRepository<ExportedReport, Long> {

    // Find by export type
    List<ExportedReport> findByExportTypeOrderByExportedAtDesc(String exportType);

    // Find by user
    Page<ExportedReport> findByExportedByIdOrderByExportedAtDesc(
            Long userId,
            Pageable pageable
    );

    // Find by patient
    List<ExportedReport> findByPatientIdOrderByExportedAtDesc(Long patientId);

    // Find by report
    List<ExportedReport> findByReportIdOrderByExportedAtDesc(Long reportId);

    // Find by treatment plan
    List<ExportedReport> findByTreatmentPlanIdOrderByExportedAtDesc(Long treatmentPlanId);

    // Find by clinical note
    List<ExportedReport> findByClinicalNoteIdOrderByExportedAtDesc(Long clinicalNoteId);

    // Find by format and type
    @Query("SELECT e FROM ExportedReport e WHERE e.format = :format " +
            "AND e.exportType = :type ORDER BY e.exportedAt DESC")
    List<ExportedReport> findByFormatAndType(
            @Param("format") String format,
            @Param("type") String type
    );

    // Find recent exports
    @Query("SELECT e FROM ExportedReport e WHERE e.exportedAt >= :since " +
            "ORDER BY e.exportedAt DESC")
    List<ExportedReport> findRecentExports(@Param("since") LocalDateTime since);

    // Find user's recent exports
    @Query("SELECT e FROM ExportedReport e WHERE e.exportedBy.id = :userId " +
            "AND e.exportedAt >= :since ORDER BY e.exportedAt DESC")
    List<ExportedReport> findUserRecentExports(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // Check if report already exported
    @Query("SELECT e FROM ExportedReport e WHERE e.report.id = :reportId " +
            "AND e.format = :format AND e.exportedBy.id = :userId " +
            "ORDER BY e.exportedAt DESC")
    List<ExportedReport> findExistingExport(
            @Param("reportId") Long reportId,
            @Param("format") String format,
            @Param("userId") Long userId
    );

    // Increment download count
    @Modifying
    @Query("UPDATE ExportedReport e SET e.downloadCount = e.downloadCount + 1, " +
            "e.lastDownloadedAt = :downloadTime WHERE e.id = :id")
    void incrementDownloadCount(
            @Param("id") Long id,
            @Param("downloadTime") LocalDateTime downloadTime
    );

    // Get export statistics
    @Query("SELECT e.exportType, e.format, COUNT(e) " +
            "FROM ExportedReport e GROUP BY e.exportType, e.format")
    List<Object[]> getExportStatistics();

    // Get user export count
    @Query("SELECT COUNT(e) FROM ExportedReport e WHERE e.exportedBy.id = :userId")
    Long countUserExports(@Param("userId") Long userId);

    // Delete old exports (cleanup)
    @Modifying
    @Query("DELETE FROM ExportedReport e WHERE e.exportedAt < :before")
    void deleteOldExports(@Param("before") LocalDateTime before);
}