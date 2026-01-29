package com.team.medaibackend.web;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import com.team.medaibackend.security.SecurityUtils;
import com.team.medaibackend.service.AuditService;
import com.team.medaibackend.service.ClinicalNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinical-notes")
public class ClinicalNoteController {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final NoteTemplateRepository noteTemplateRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final StudyRepository studyRepository;
    private final ClinicalNoteService clinicalNoteService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public ClinicalNoteController(
            ClinicalNoteRepository clinicalNoteRepository,
            NoteTemplateRepository noteTemplateRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            StudyRepository studyRepository,
            ClinicalNoteService clinicalNoteService,
            SecurityUtils securityUtils,
            AuditService auditService) {
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.noteTemplateRepository = noteTemplateRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.studyRepository = studyRepository;
        this.clinicalNoteService = clinicalNoteService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    // ============================================================================
    // CLINICAL NOTES CRUD
    // ============================================================================

    @GetMapping
    public ResponseEntity<?> getAllNotes(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String noteType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sinceDate
    ) {
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            List<ClinicalNote> notes;

            if (patientId != null) {
                if (noteType != null) {
                    notes = clinicalNoteRepository.findByPatientIdAndNoteTypeOrderByNoteDateDesc(patientId, noteType);
                } else if (status != null) {
                    notes = clinicalNoteRepository.findByPatientIdAndStatusOrderByNoteDateDesc(patientId, status);
                } else {
                    notes = clinicalNoteRepository.findByPatientIdOrderByNoteDateDesc(patientId);
                }
            } else if (sinceDate != null) {
                LocalDate since = LocalDate.parse(sinceDate);
                if ("DOCTOR".equals(currentUser.getPrimaryRole())) {
                    notes = clinicalNoteRepository.findRecentNotesByDoctor(currentUser.getId(), since);
                } else {
                    notes = clinicalNoteRepository.findRecentNotes(since);
                }
            } else if ("DOCTOR".equals(currentUser.getPrimaryRole())) {
                notes = clinicalNoteRepository.findByDoctorIdOrderByNoteDateDesc(currentUser.getId());
            } else {
                notes = clinicalNoteRepository.findAll();
            }

            List<Map<String, Object>> result = notes.stream()
                    .map(this::buildNoteDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FIXED VERSION - Line 98-184
// Only showing the createNote method with the fix

    @PostMapping
    public ResponseEntity<?> createNote(@RequestBody Map<String, Object> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        // Verify doctor role
        if (!"DOCTOR".equals(doctor.getPrimaryRole()) && !securityUtils.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Only doctors can create clinical notes"));
        }

        try {
            // ✅ ADD THIS NULL CHECK:
            if (request.get("patientId") == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Patient ID is required. Please select a patient.")
                );
            }

            // Get patient
            Long patientId = Long.valueOf(request.get("patientId").toString());
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            // Create note
            ClinicalNote note = new ClinicalNote();
            note.setPatient(patient);
            note.setDoctor(doctor);

            // Note details
            note.setNoteType((String) request.get("noteType"));
            note.setTitle((String) request.get("title"));

            // SOAP fields
            if (request.containsKey("subjective"))
                note.setSubjective((String) request.get("subjective"));
            if (request.containsKey("objective"))
                note.setObjective((String) request.get("objective"));
            if (request.containsKey("assessment"))
                note.setAssessment((String) request.get("assessment"));
            if (request.containsKey("plan"))
                note.setPlan((String) request.get("plan"));

            // General content
            if (request.containsKey("content"))
                note.setContent((String) request.get("content"));

            // Vitals
            if (request.containsKey("vitals"))
                note.setVitals((Map<String, Object>) request.get("vitals"));

            // Optional relationships
            if (request.containsKey("appointmentId") && request.get("appointmentId") != null) {
                Long appointmentId = Long.valueOf(request.get("appointmentId").toString());
                Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
                note.setAppointment(appointment);
            }

            if (request.containsKey("studyId") && request.get("studyId") != null) {
                Long studyId = Long.valueOf(request.get("studyId").toString());
                Study study = studyRepository.findById(studyId).orElse(null);
                note.setStudy(study);
            }

            // Date
            if (request.containsKey("noteDate")) {
                note.setNoteDate(LocalDate.parse(request.get("noteDate").toString()));
            }

            // Validate
            Map<String, String> errors = clinicalNoteService.validateNote(note);
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("errors", errors));
            }

            // Generate UID
            note.setNoteUid(clinicalNoteService.generateNoteUid(note.getNoteDate()));

            // Save
            ClinicalNote saved = clinicalNoteRepository.save(note);

            // Audit log
            auditService.log(
                    "CLINICAL_NOTE_CREATED",
                    "CLINICAL_NOTE",
                    saved.getId().toString(),
                    doctor.getId(),
                    doctor.getUsername()
            );

            return ResponseEntity.ok(buildNoteDto(saved));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid patient ID format")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNoteDetail(@PathVariable Long id) {
        ClinicalNote note = clinicalNoteRepository.findById(id).orElse(null);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildNoteDto(note));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            ClinicalNote note = clinicalNoteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Note not found"));

            // Verify ownership
            if (!note.getDoctor().getId().equals(doctor.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "You can only edit your own notes"));
            }

            // Cannot edit finalized notes
            if (Boolean.TRUE.equals(note.getFinalized())) {
                return ResponseEntity.status(403).body(Map.of("message", "Cannot edit finalized notes"));
            }

            // Update fields
            if (request.containsKey("title"))
                note.setTitle((String) request.get("title"));
            if (request.containsKey("subjective"))
                note.setSubjective((String) request.get("subjective"));
            if (request.containsKey("objective"))
                note.setObjective((String) request.get("objective"));
            if (request.containsKey("assessment"))
                note.setAssessment((String) request.get("assessment"));
            if (request.containsKey("plan"))
                note.setPlan((String) request.get("plan"));
            if (request.containsKey("content"))
                note.setContent((String) request.get("content"));
            if (request.containsKey("vitals"))
                note.setVitals((Map<String, Object>) request.get("vitals"));

            // Validate
            Map<String, String> errors = clinicalNoteService.validateNote(note);
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("errors", errors));
            }

            ClinicalNote saved = clinicalNoteRepository.save(note);

            auditService.log(
                    "CLINICAL_NOTE_UPDATED",
                    "CLINICAL_NOTE",
                    saved.getId().toString(),
                    doctor.getId(),
                    doctor.getUsername()
            );

            return ResponseEntity.ok(buildNoteDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            clinicalNoteService.deleteNote(id, doctor);
            return ResponseEntity.ok(Map.of("message", "Note deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<?> finalizeNote(@PathVariable Long id) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            ClinicalNote finalized = clinicalNoteService.finalizeNote(id, doctor);
            return ResponseEntity.ok(buildNoteDto(finalized));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // PATIENT NOTES
    // ============================================================================

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientNotes(@PathVariable(required = false) Long patientId) {
        // Validate patientId
        if (patientId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Patient ID is required")
            );
        }

        try {
            List<ClinicalNote> notes = clinicalNoteRepository.findByPatientIdOrderByNoteDateDesc(patientId);

            List<Map<String, Object>> result = notes.stream()
                    .map(this::buildNoteDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Failed to load patient notes: " + e.getMessage())
            );
        }
    }

    @GetMapping("/patient/{patientId}/stats")
    public ResponseEntity<?> getPatientNoteStats(@PathVariable Long patientId) {
        try {
            Map<String, Object> stats = clinicalNoteService.getPatientNoteStats(patientId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // APPOINTMENT NOTES
    // ============================================================================

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getAppointmentNotes(@PathVariable Long appointmentId) {
        try {
            List<ClinicalNote> notes = clinicalNoteRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId);

            List<Map<String, Object>> result = notes.stream()
                    .map(this::buildNoteDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // SEARCH
    // ============================================================================

    @GetMapping("/search")
    public ResponseEntity<?> searchNotes(
            @RequestParam String q,
            @RequestParam(required = false) Long patientId
    ) {
        try {
            List<ClinicalNote> notes;

            if (patientId != null) {
                notes = clinicalNoteRepository.searchNotesByPatient(patientId, q);
            } else {
                notes = clinicalNoteRepository.searchNotes(q);
            }

            List<Map<String, Object>> result = notes.stream()
                    .map(this::buildNoteDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // TEMPLATES
    // ============================================================================

    @GetMapping("/templates")
    public ResponseEntity<?> getAllTemplates(@RequestParam(required = false) String noteType) {
        try {
            List<NoteTemplate> templates;

            if (noteType != null) {
                templates = noteTemplateRepository.findAllByNoteType(noteType);
            } else {
                templates = noteTemplateRepository.findAll();
            }

            List<Map<String, Object>> result = templates.stream()
                    .map(this::buildTemplateDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        NoteTemplate template = noteTemplateRepository.findById(id).orElse(null);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildTemplateDto(template));
    }

    @PostMapping("/templates/{id}/apply")
    public ResponseEntity<?> applyTemplate(@PathVariable Long id) {
        try {
            Map<String, Object> noteData = clinicalNoteService.applyTemplate(id);
            if (noteData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(noteData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private Map<String, Object> buildNoteDto(ClinicalNote note) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("id", note.getId());
        dto.put("noteUid", note.getNoteUid());
        dto.put("noteType", note.getNoteType());
        dto.put("title", note.getTitle());
        dto.put("subjective", note.getSubjective());
        dto.put("objective", note.getObjective());
        dto.put("assessment", note.getAssessment());
        dto.put("plan", note.getPlan());
        dto.put("content", note.getContent());
        dto.put("vitals", note.getVitals());
        dto.put("status", note.getStatus());
        dto.put("finalized", note.getFinalized());
        dto.put("finalizedAt", note.getFinalizedAt());
        dto.put("noteDate", note.getNoteDate());
        dto.put("createdAt", note.getCreatedAt());
        dto.put("updatedAt", note.getUpdatedAt());

        // Patient info
        if (note.getPatient() != null) {
            dto.put("patientId", note.getPatient().getId());
            dto.put("patientName", note.getPatient().getName());
            dto.put("patientIdNumber", note.getPatient().getPatientId());
        }

        // Doctor info
        if (note.getDoctor() != null) {
            dto.put("doctorId", note.getDoctor().getId());
            dto.put("doctorName", note.getDoctor().getFullName());
        }

        // Optional relationships
        if (note.getAppointment() != null) {
            dto.put("appointmentId", note.getAppointment().getId());
        }

        if (note.getStudy() != null) {
            dto.put("studyId", note.getStudy().getId());
        }

        return dto;
    }

    private Map<String, Object> buildTemplateDto(NoteTemplate template) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("id", template.getId());
        dto.put("name", template.getName());
        dto.put("noteType", template.getNoteType());
        dto.put("description", template.getDescription());
        dto.put("subjectiveTemplate", template.getSubjectiveTemplate());
        dto.put("objectiveTemplate", template.getObjectiveTemplate());
        dto.put("assessmentTemplate", template.getAssessmentTemplate());
        dto.put("planTemplate", template.getPlanTemplate());
        dto.put("contentTemplate", template.getContentTemplate());
        dto.put("isSystem", template.getIsSystem());
        dto.put("specialty", template.getSpecialty());
        dto.put("createdAt", template.getCreatedAt());

        if (template.getCreatedBy() != null) {
            dto.put("createdBy", template.getCreatedBy().getFullName());
        }

        return dto;
    }
}