package com.team.medaibackend.service;

import com.team.medaibackend.entity.ClinicalNote;
import com.team.medaibackend.entity.NoteTemplate;
import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.ClinicalNoteRepository;
import com.team.medaibackend.repository.NoteTemplateRepository;
import com.team.medaibackend.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClinicalNoteService {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final NoteTemplateRepository noteTemplateRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public ClinicalNoteService(
            ClinicalNoteRepository clinicalNoteRepository,
            NoteTemplateRepository noteTemplateRepository,
            PatientRepository patientRepository,
            AuditService auditService) {
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.noteTemplateRepository = noteTemplateRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    /**
     * Generate unique note UID
     * Format: NOTE-YYYYMMDD-XXXXXX
     */
    public String generateNoteUid(LocalDate noteDate) {
        String datePrefix = "NOTE-" + noteDate.toString().replace("-", "");

        // Find count for today
        long count = clinicalNoteRepository.count();
        int sequence = (int) (count % 1000000) + 1;

        return String.format("%s-%06d", datePrefix, sequence);
    }

    /**
     * Create note from template
     */
    public Map<String, Object> applyTemplate(Long templateId) {
        NoteTemplate template = noteTemplateRepository.findById(templateId).orElse(null);
        if (template == null) {
            return null;
        }

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("noteType", template.getNoteType());
        noteData.put("title", template.getName());

        if ("soap".equals(template.getNoteType())) {
            noteData.put("subjective", template.getSubjectiveTemplate());
            noteData.put("objective", template.getObjectiveTemplate());
            noteData.put("assessment", template.getAssessmentTemplate());
            noteData.put("plan", template.getPlanTemplate());
        } else {
            noteData.put("content", template.getContentTemplate());
        }

        return noteData;
    }

    /**
     * Finalize note (lock it from editing)
     */
    @Transactional
    public ClinicalNote finalizeNote(Long noteId, User doctor) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        // Verify ownership
        if (!note.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("You can only finalize your own notes");
        }

        // Check if already finalized
        if (Boolean.TRUE.equals(note.getFinalized())) {
            throw new RuntimeException("Note is already finalized");
        }

        note.setStatus("finalized");
        note.setFinalized(true);
        note.setFinalizedAt(LocalDateTime.now());

        ClinicalNote saved = clinicalNoteRepository.save(note);

        // Audit log
        auditService.log(
                "CLINICAL_NOTE_FINALIZED",
                "CLINICAL_NOTE",
                saved.getId().toString(),
                doctor.getId(),
                doctor.getUsername()
        );

        return saved;
    }

    /**
     * Auto-save draft note
     */
    @Transactional
    public ClinicalNote autoSaveDraft(ClinicalNote note) {
        // Only save if still draft
        if (!"draft".equals(note.getStatus())) {
            throw new RuntimeException("Cannot auto-save finalized notes");
        }

        return clinicalNoteRepository.save(note);
    }

    /**
     * Delete note (only drafts can be deleted)
     */
    @Transactional
    public void deleteNote(Long noteId, User doctor) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        // Verify ownership
        if (!note.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("You can only delete your own notes");
        }

        // Only drafts can be deleted
        if (Boolean.TRUE.equals(note.getFinalized())) {
            throw new RuntimeException("Cannot delete finalized notes");
        }

        clinicalNoteRepository.delete(note);

        // Audit log
        auditService.log(
                "CLINICAL_NOTE_DELETED",
                "CLINICAL_NOTE",
                noteId.toString(),
                doctor.getId(),
                doctor.getUsername()
        );
    }

    /**
     * Get patient note statistics
     */
    public Map<String, Object> getPatientNoteStats(Long patientId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalNotes", clinicalNoteRepository.countByPatientId(patientId));
        stats.put("draftNotes", clinicalNoteRepository.countByPatientIdAndStatus(patientId, "draft"));
        stats.put("finalizedNotes", clinicalNoteRepository.countByPatientIdAndStatus(patientId, "finalized"));

        // Count by type
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient != null) {
            List<ClinicalNote> allNotes = clinicalNoteRepository.findByPatientIdOrderByNoteDateDesc(patientId);

            long soapCount = allNotes.stream().filter(n -> "soap".equals(n.getNoteType())).count();
            long progressCount = allNotes.stream().filter(n -> "progress".equals(n.getNoteType())).count();
            long consultCount = allNotes.stream().filter(n -> "consultation".equals(n.getNoteType())).count();

            stats.put("soapNotes", soapCount);
            stats.put("progressNotes", progressCount);
            stats.put("consultationNotes", consultCount);
        }

        return stats;
    }

    /**
     * Get doctor note statistics
     */
    public Map<String, Object> getDoctorNoteStats(Long doctorId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalNotes", clinicalNoteRepository.countByDoctorId(doctorId));

        // Recent notes (last 7 days)
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        List<ClinicalNote> recentNotes = clinicalNoteRepository.findRecentNotesByDoctor(doctorId, weekAgo);
        stats.put("notesThisWeek", recentNotes.size());

        // Draft notes
        List<ClinicalNote> drafts = clinicalNoteRepository.findDraftsByDoctor(doctorId);
        stats.put("draftNotes", drafts.size());

        return stats;
    }

    /**
     * Validate note content before saving
     */
    public Map<String, String> validateNote(ClinicalNote note) {
        Map<String, String> errors = new HashMap<>();

        // Check required fields
        if (note.getNoteType() == null || note.getNoteType().trim().isEmpty()) {
            errors.put("noteType", "Note type is required");
        }

        if (note.getPatient() == null) {
            errors.put("patient", "Patient is required");
        }

        if (note.getDoctor() == null) {
            errors.put("doctor", "Doctor is required");
        }

        // Validate SOAP notes have content
        if ("soap".equals(note.getNoteType())) {
            if ((note.getSubjective() == null || note.getSubjective().trim().isEmpty()) &&
                    (note.getObjective() == null || note.getObjective().trim().isEmpty()) &&
                    (note.getAssessment() == null || note.getAssessment().trim().isEmpty()) &&
                    (note.getPlan() == null || note.getPlan().trim().isEmpty())) {
                errors.put("soapContent", "SOAP note must have at least one section filled");
            }
        } else {
            // Other note types need content
            if (note.getContent() == null || note.getContent().trim().isEmpty()) {
                errors.put("content", "Note content is required");
            }
        }

        return errors;
    }

    /**
     * Update note timestamp (for auto-save tracking)
     */
    @Transactional
    public void updateNoteTimestamp(Long noteId) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId).orElse(null);
        if (note != null && "draft".equals(note.getStatus())) {
            note.setUpdatedAt(LocalDateTime.now());
            clinicalNoteRepository.save(note);
        }
    }
}