package com.team.medaibackend.repository;

import com.team.medaibackend.entity.NoteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteTemplateRepository extends JpaRepository<NoteTemplate, Long> {

    // Find all system templates
    List<NoteTemplate> findByIsSystemTrueOrderByNameAsc();

    // Find all user templates
    @Query("SELECT t FROM NoteTemplate t WHERE t.isSystem = false ORDER BY t.name ASC")
    List<NoteTemplate> findUserTemplates();

    // Find templates by note type
    List<NoteTemplate> findByNoteTypeOrderByNameAsc(String noteType);

    // Find system templates by note type
    @Query("SELECT t FROM NoteTemplate t WHERE t.noteType = :noteType " +
            "AND t.isSystem = true ORDER BY t.name ASC")
    List<NoteTemplate> findSystemTemplatesByType(@Param("noteType") String noteType);

    // Find templates by specialty
    List<NoteTemplate> findBySpecialtyOrderByNameAsc(String specialty);

    // Find templates by creator
    @Query("SELECT t FROM NoteTemplate t WHERE t.createdBy.id = :userId ORDER BY t.createdAt DESC")
    List<NoteTemplate> findByCreator(@Param("userId") Long userId);

    // Find templates by name (case-insensitive search)
    @Query("SELECT t FROM NoteTemplate t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "ORDER BY t.name ASC")
    List<NoteTemplate> searchByName(@Param("name") String name);

    // Get all available templates for a note type (system + user)
    @Query("SELECT t FROM NoteTemplate t WHERE t.noteType = :noteType " +
            "ORDER BY t.isSystem DESC, t.name ASC")
    List<NoteTemplate> findAllByNoteType(@Param("noteType") String noteType);

    // Count templates by type
    Long countByNoteType(String noteType);

    // Check if template name exists (for custom templates)
    boolean existsByNameAndCreatedById(String name, Long createdById);
}