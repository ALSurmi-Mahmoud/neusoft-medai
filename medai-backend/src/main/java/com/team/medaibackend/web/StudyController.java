package com.team.medaibackend.web;

import com.team.medaibackend.entity.Study;
import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.repository.StudyRepository;
import com.team.medaibackend.repository.PatientRepository;
import com.team.medaibackend.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/studies")
public class StudyController {

    private final StudyRepository studyRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public StudyController(StudyRepository studyRepository,
                           PatientRepository patientRepository,
                           AuditService auditService) {
        this.studyRepository = studyRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStudies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("studyDate").descending());
        Page<Study> studyPage;

        if (modality != null && !modality.isEmpty()) {
            studyPage = studyRepository.findByModality(modality, pageable);
        } else if (status != null && !status.isEmpty()) {
            studyPage = studyRepository.findByStatus(status, pageable);
        } else {
            studyPage = studyRepository.findAll(pageable);
        }

        List<Map<String, Object>> studyList = new ArrayList<>();
        for (Study study : studyPage.getContent()) {
            studyList.add(toDto(study));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", studyList);
        response.put("totalElements", studyPage.getTotalElements());
        response.put("totalPages", studyPage.getTotalPages());
        response.put("number", studyPage.getNumber());
        response.put("size", studyPage.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStudy(@PathVariable Long id) {
        return studyRepository.findById(id)
                .map(study -> ResponseEntity.ok(toDetailDto(study)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/uid/{studyUid}")
    public ResponseEntity<?> getStudyByUid(@PathVariable String studyUid) {
        return studyRepository.findByStudyUid(studyUid)
                .map(study -> ResponseEntity.ok(toDetailDto(study)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createStudy(@RequestBody Map<String, Object> request) {
        try {
            Study study = new Study();

            study.setStudyUid((String) request.get("studyUid"));
            study.setDescription((String) request.get("description"));
            study.setModality((String) request.get("modality"));
            study.setStatus("uploaded");
            study.setStudyDate(LocalDateTime.now());

            // Link to patient if provided
            if (request.containsKey("patientId")) {
                Long patientId = Long.valueOf(request.get("patientId").toString());
                Patient patient = patientRepository.findById(patientId).orElse(null);
                study.setPatient(patient);
            }

            Study saved = studyRepository.save(study);
            auditService.logAnonymous("CREATE", "STUDY", saved.getId().toString());

            return ResponseEntity.ok(Map.of(
                    "message", "Study created",
                    "id", saved.getId(),
                    "studyUid", saved.getStudyUid()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudy(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return studyRepository.findById(id).map(study -> {
            if (request.containsKey("description")) {
                study.setDescription((String) request.get("description"));
            }
            if (request.containsKey("status")) {
                study.setStatus((String) request.get("status"));
            }
            if (request.containsKey("modality")) {
                study.setModality((String) request.get("modality"));
            }

            studyRepository.save(study);
            auditService.logAnonymous("UPDATE", "STUDY", id.toString());

            return ResponseEntity.ok(Map.of("message", "Study updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudy(@PathVariable Long id) {
        return studyRepository.findById(id).map(study -> {
            studyRepository.delete(study);
            auditService.logAnonymous("DELETE", "STUDY", id.toString());
            return ResponseEntity.ok(Map.of("message", "Study deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", studyRepository.count());
        stats.put("uploaded", studyRepository.countByStatus("uploaded"));
        stats.put("processing", studyRepository.countByStatus("processing"));
        stats.put("completed", studyRepository.countByStatus("completed"));
        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> toDto(Study study) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", study.getId());
        dto.put("studyUid", study.getStudyUid());
        dto.put("description", study.getDescription());
        dto.put("modality", study.getModality());
        dto.put("status", study.getStatus());
        dto.put("studyDate", study.getStudyDate() != null ? study.getStudyDate().toString() : null);
        dto.put("createdAt", study.getCreatedAt() != null ? study.getCreatedAt().toString() : null);

        if (study.getPatient() != null) {
            dto.put("patientId", study.getPatient().getId());
            dto.put("patientName", study.getPatient().getName());
        }

        return dto;
    }

    private Map<String, Object> toDetailDto(Study study) {
        Map<String, Object> dto = toDto(study);

        // Add series count
        if (study.getSeriesList() != null) {
            dto.put("seriesCount", study.getSeriesList().size());
        } else {
            dto.put("seriesCount", 0);
        }

        return dto;
    }
}