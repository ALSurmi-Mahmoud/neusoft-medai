package com.team.medaibackend.repository;

import com.team.medaibackend.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    // Find by note UID
    Optional<ClinicalNote> findByNoteUid(String noteUid);

    // Find all notes for a patient
    List<ClinicalNote> findByPatientIdOrderByNoteDateDesc(Long patientId);

    // Find notes by patient and status
    List<ClinicalNote> findByPatientIdAndStatusOrderByNoteDateDesc(Long patientId, String status);

    // Find all notes by a doctor
    List<ClinicalNote> findByDoctorIdOrderByNoteDateDesc(Long doctorId);

    // Find notes by appointment
    List<ClinicalNote> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    // Find notes by study
    List<ClinicalNote> findByStudyIdOrderByCreatedAtDesc(Long studyId);

    // Find notes by type
    List<ClinicalNote> findByNoteTypeOrderByNoteDateDesc(String noteType);

    // Find notes by patient and type
    List<ClinicalNote> findByPatientIdAndNoteTypeOrderByNoteDateDesc(Long patientId, String noteType);

    // Find draft notes by doctor
    @Query("SELECT n FROM ClinicalNote n WHERE n.doctor.id = :doctorId AND n.status = 'draft' ORDER BY n.updatedAt DESC")
    List<ClinicalNote> findDraftsByDoctor(@Param("doctorId") Long doctorId);

    // Find finalized notes by patient
    @Query("SELECT n FROM ClinicalNote n WHERE n.patient.id = :patientId AND n.finalized = true ORDER BY n.noteDate DESC")
    List<ClinicalNote> findFinalizedByPatient(@Param("patientId") Long patientId);

    // Find notes by date range
    @Query("SELECT n FROM ClinicalNote n WHERE n.patient.id = :patientId " +
            "AND n.noteDate BETWEEN :startDate AND :endDate ORDER BY n.noteDate DESC")
    List<ClinicalNote> findByPatientAndDateRange(
            @Param("patientId") Long patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Full-text search across all note content
    @Query(value = "SELECT * FROM clinical_notes " +
            "WHERE to_tsvector('english', " +
            "coalesce(title, '') || ' ' || " +
            "coalesce(subjective, '') || ' ' || " +
            "coalesce(objective, '') || ' ' || " +
            "coalesce(assessment, '') || ' ' || " +
            "coalesce(plan, '') || ' ' || " +
            "coalesce(content, '')) @@ plainto_tsquery('english', :query) " +
            "ORDER BY note_date DESC",
            nativeQuery = true)
    List<ClinicalNote> searchNotes(@Param("query") String query);

    // Search notes by patient and query
    @Query(value = "SELECT * FROM clinical_notes " +
            "WHERE patient_id = :patientId " +
            "AND to_tsvector('english', " +
            "coalesce(title, '') || ' ' || " +
            "coalesce(subjective, '') || ' ' || " +
            "coalesce(objective, '') || ' ' || " +
            "coalesce(assessment, '') || ' ' || " +
            "coalesce(plan, '') || ' ' || " +
            "coalesce(content, '')) @@ plainto_tsquery('english', :query) " +
            "ORDER BY note_date DESC",
            nativeQuery = true)
    List<ClinicalNote> searchNotesByPatient(@Param("patientId") Long patientId, @Param("query") String query);

    // Count notes by patient
    Long countByPatientId(Long patientId);

    // Count notes by patient and status
    Long countByPatientIdAndStatus(Long patientId, String status);

    // Count notes by doctor
    Long countByDoctorId(Long doctorId);

    // Get recent notes (last N days)
    @Query("SELECT n FROM ClinicalNote n WHERE n.noteDate >= :sinceDate ORDER BY n.noteDate DESC")
    List<ClinicalNote> findRecentNotes(@Param("sinceDate") LocalDate sinceDate);

    // Get recent notes by doctor
    @Query("SELECT n FROM ClinicalNote n WHERE n.doctor.id = :doctorId " +
            "AND n.noteDate >= :sinceDate ORDER BY n.noteDate DESC")
    List<ClinicalNote> findRecentNotesByDoctor(
            @Param("doctorId") Long doctorId,
            @Param("sinceDate") LocalDate sinceDate
    );

    // Find notes modified recently (for auto-save tracking)
    @Query("SELECT n FROM ClinicalNote n WHERE n.status = 'draft' " +
            "AND n.updatedAt >= :since ORDER BY n.updatedAt DESC")
    List<ClinicalNote> findRecentlyModifiedDrafts(@Param("since") LocalDate since);
}