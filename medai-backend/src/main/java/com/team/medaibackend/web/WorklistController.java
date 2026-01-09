package com.team.medaibackend.web;

import com.team.medaibackend.entity.Study;
import com.team.medaibackend.repository.StudyRepository;
import com.team.medaibackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/worklist")
public class WorklistController {

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;

    public WorklistController(StudyRepository studyRepository, UserRepository userRepository) {
        this.studyRepository = studyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWorklist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Get studies that need review
        Page<Study> studies;
        if (status != null && !status.isEmpty()) {
            studies = studyRepository.findByStatus(status, pageable);
        } else {
            // Default: show all studies as worklist items
            studies = studyRepository.findAll(pageable);
        }

        List<Map<String, Object>> worklistItems = new ArrayList<>();
        for (Study study : studies.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", study.getId());
            item.put("studyUid", study.getStudyUid());
            item.put("studyDate", study.getStudyDate() != null ? study.getStudyDate().toString() : null);
            item.put("modality", study.getModality());
            item.put("description", study.getDescription());
            item.put("status", study.getStatus() != null ? study.getStatus() : "pending");

            // Determine priority based on modality
            String itemPriority = determinePriority(study);
            item.put("priority", itemPriority);

            if (study.getPatient() != null) {
                item.put("patientId", study.getPatient().getId());
                item.put("patientName", study.getPatient().getName());
            } else {
                item.put("patientName", "Unknown Patient");
            }

            item.put("taskType", getTaskType(study.getStatus()));

            worklistItems.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", worklistItems);
        response.put("totalElements", studies.getTotalElements());
        response.put("totalPages", studies.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWorklistStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = studyRepository.count();
        long pending = studyRepository.countByStatus("pending");
        long uploaded = studyRepository.countByStatus("uploaded");
        long completed = studyRepository.countByStatus("completed");

        stats.put("total", total);
        stats.put("pending", pending + uploaded); // pending review
        stats.put("inProgress", 0);
        stats.put("completed", completed);
        stats.put("urgent", 0); // Can be calculated based on criteria

        return ResponseEntity.ok(stats);
    }

    private String determinePriority(Study study) {
        if (study.getModality() == null) return "normal";

        // CT and MR are typically higher priority
        if ("CT".equals(study.getModality()) || "MR".equals(study.getModality())) {
            return "high";
        }
        return "normal";
    }

    private String getTaskType(String status) {
        if (status == null) return "Review Required";

        switch (status) {
            case "uploaded": return "New Study - Review Required";
            case "pending": return "Pending Analysis";
            case "processing": return "AI Processing";
            case "completed": return "Completed";
            default: return "Review Required";
        }
    }
}