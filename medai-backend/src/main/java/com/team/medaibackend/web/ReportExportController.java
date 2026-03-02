package com.team.medaibackend.web;

import com.team.medaibackend.entity.ExportedReport;
import com.team.medaibackend.service.PatientRecordExportService;
import com.team.medaibackend.service.ReportExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/export")
public class ReportExportController {

    private final ReportExportService exportService;
    private final PatientRecordExportService patientRecordService;

    public ReportExportController(
            ReportExportService exportService,
            PatientRecordExportService patientRecordService) {
        this.exportService = exportService;
        this.patientRecordService = patientRecordService;
    }

    @PostMapping("/report/{reportId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> exportReport(
            @PathVariable Long reportId,
            @RequestParam String format,
            @RequestParam(required = false) Long templateId) {
        try {
            ExportedReport export = exportService.exportReport(reportId, format, templateId);

            Map<String, Object> response = new HashMap<>();
            response.put("exportId", export.getId());
            response.put("fileName", export.getFileName());
            response.put("fileSize", export.getFileSize());
            response.put("format", export.getFormat());
            response.put("message", "Report exported successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/treatment-plan/{planId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> exportTreatmentPlan(
            @PathVariable Long planId,
            @RequestParam String format,
            @RequestParam(required = false) Long templateId) {
        try {
            ExportedReport export = exportService.exportTreatmentPlan(planId, format, templateId);

            Map<String, Object> response = new HashMap<>();
            response.put("exportId", export.getId());
            response.put("fileName", export.getFileName());
            response.put("fileSize", export.getFileSize());
            response.put("format", export.getFormat());
            response.put("message", "Treatment plan exported successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/clinical-note/{noteId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> exportClinicalNote(
            @PathVariable Long noteId,
            @RequestParam String format,
            @RequestParam(required = false) Long templateId) {
        try {
            ExportedReport export = exportService.exportClinicalNote(noteId, format, templateId);

            Map<String, Object> response = new HashMap<>();
            response.put("exportId", export.getId());
            response.put("fileName", export.getFileName());
            response.put("fileSize", export.getFileSize());
            response.put("format", export.getFormat());
            response.put("message", "Clinical note exported successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/patient-summary/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> exportPatientSummary(
            @PathVariable Long patientId,
            @RequestParam String format) {
        try {
            ExportedReport export = patientRecordService.exportPatientSummary(patientId, format);

            Map<String, Object> response = new HashMap<>();
            response.put("exportId", export.getId());
            response.put("fileName", export.getFileName());
            response.put("fileSize", export.getFileSize());
            response.put("format", export.getFormat());
            response.put("message", "Patient summary exported successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/download/{exportId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<Resource> downloadExport(@PathVariable Long exportId) {
        try {
            byte[] fileContent = exportService.downloadExport(exportId);

            List<ExportedReport> exports = exportService.getUserExportHistory();
            ExportedReport export = exports.stream()
                    .filter(e -> e.getId().equals(exportId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Export not found"));

            ByteArrayResource resource = new ByteArrayResource(fileContent);

            MediaType mediaType = export.getFormat().equals("pdf") ?
                    MediaType.APPLICATION_PDF :
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + export.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getExportHistory() {
        try {
            List<ExportedReport> exports = exportService.getUserExportHistory();

            List<Map<String, Object>> result = exports.stream()
                    .map(this::buildExportDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> buildExportDto(ExportedReport export) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", export.getId());
        dto.put("fileName", export.getFileName());
        dto.put("fileSize", export.getFileSize());
        dto.put("format", export.getFormat());
        dto.put("exportType", export.getExportType());
        dto.put("title", export.getTitle());
        dto.put("exportedAt", export.getExportedAt());
        dto.put("downloadCount", export.getDownloadCount());
        return dto;
    }
}