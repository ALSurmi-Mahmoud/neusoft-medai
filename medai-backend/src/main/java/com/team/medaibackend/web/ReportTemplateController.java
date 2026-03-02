package com.team.medaibackend.web;

import com.team.medaibackend.entity.ReportTemplate;
import com.team.medaibackend.service.ReportTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report-templates")
public class ReportTemplateController {

    private final ReportTemplateService templateService;

    public ReportTemplateController(ReportTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getAllTemplates(@RequestParam(required = false) String category) {
        try {
            List<ReportTemplate> templates;

            if (category != null && !category.isEmpty()) {
                templates = templateService.getTemplatesByCategory(category);
            } else {
                templates = templateService.getAccessibleTemplates();
            }

            List<Map<String, Object>> result = templates.stream()
                    .map(this::buildTemplateDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        try {
            ReportTemplate template = templateService.getTemplateById(id);
            return ResponseEntity.ok(buildTemplateDto(template));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> request) {
        try {
            ReportTemplate template = new ReportTemplate();
            template.setName((String) request.get("name"));
            template.setCategory((String) request.get("category"));
            template.setDescription((String) request.get("description"));
            template.setContent((String) request.get("content"));

            if (request.get("headerHtml") != null) {
                template.setHeaderHtml((String) request.get("headerHtml"));
            }
            if (request.get("footerHtml") != null) {
                template.setFooterHtml((String) request.get("footerHtml"));
            }

            ReportTemplate saved = templateService.createTemplate(template);
            return ResponseEntity.ok(buildTemplateDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            ReportTemplate updates = new ReportTemplate();
            if (request.get("name") != null) updates.setName((String) request.get("name"));
            if (request.get("description") != null) updates.setDescription((String) request.get("description"));
            if (request.get("content") != null) updates.setContent((String) request.get("content"));
            if (request.get("category") != null) updates.setCategory((String) request.get("category"));

            ReportTemplate saved = templateService.updateTemplate(id, updates);
            return ResponseEntity.ok(buildTemplateDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok(Map.of("message", "Template deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> buildTemplateDto(ReportTemplate template) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", template.getId());
        dto.put("name", template.getName());
        dto.put("category", template.getCategory());
        dto.put("description", template.getDescription());
        dto.put("content", template.getContent());
        dto.put("isSystem", template.getIsSystem());
        dto.put("isActive", template.getIsActive());
        dto.put("usageCount", template.getUsageCount());
        dto.put("variables", template.getVariables());
        dto.put("createdAt", template.getCreatedAt());
        return dto;
    }
}