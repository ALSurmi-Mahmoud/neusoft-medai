package com.team.medaibackend.web;

import com.team.medaibackend.dto.CreateReportRequest;
import com.team.medaibackend.dto.ReportDto;
import com.team.medaibackend.dto.UpdateReportRequest;
import com.team.medaibackend.entity.Report;
import com.team.medaibackend.repository.ReportRepository;
import com.team.medaibackend.service.AuditService;
import com.team.medaibackend.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportRepository reportRepository;
    private final AuditService auditService;

    public ReportController(ReportService reportService,
                            ReportRepository reportRepository,
                            AuditService auditService) {
        this.reportService = reportService;
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new diagnostic report.
     */
    @PostMapping
    public ResponseEntity<ReportDto> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @RequestParam(defaultValue = "1") Long authorId) {

        ReportDto report = reportService.createReport(request, authorId);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all reports with pagination.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ReportDto> reports = reportService.getAllReports(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", reports.getContent());
        response.put("totalElements", reports.getTotalElements());
        response.put("totalPages", reports.getTotalPages());
        response.put("currentPage", reports.getNumber());
        response.put("pageSize", reports.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * Get report by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> getReport(@PathVariable Long id) {
        ReportDto report = reportService.getReport(id);
        return ResponseEntity.ok(report);
    }

    /**
     * Get report by UID.
     */
    @GetMapping("/uid/{reportUid}")
    public ResponseEntity<ReportDto> getReportByUid(@PathVariable String reportUid) {
        ReportDto report = reportService.getReportByUid(reportUid);
        return ResponseEntity.ok(report);
    }

    /**
     * Get reports for a specific study.
     */
    @GetMapping("/study/{studyId}")
    public ResponseEntity<List<ReportDto>> getReportsByStudy(@PathVariable Long studyId) {
        List<ReportDto> reports = reportService.getReportsByStudy(studyId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Update an existing report.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReportDto> updateReport(
            @PathVariable Long id,
            @RequestBody UpdateReportRequest request,
            @RequestParam(defaultValue = "1") Long userId) {

        ReportDto report = reportService.updateReport(id, request, userId);
        return ResponseEntity.ok(report);
    }

    /**
     * Finalize a report (makes it read-only).
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<ReportDto> finalizeReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId) {

        ReportDto report = reportService.finalizeReport(id, userId);
        return ResponseEntity.ok(report);
    }

    /**
     * Delete a draft report.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId) {

        reportService.deleteReport(id, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Report deleted successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportReportPdf(@PathVariable Long id) {
        Optional<Report> reportOpt = reportRepository.findById(id);

        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Report report = reportOpt.get();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
        html.append("h1 { color: #333; border-bottom: 2px solid #409eff; padding-bottom: 10px; }");
        html.append("h2 { color: #409eff; margin-top: 20px; }");
        html.append(".meta { background: #f5f5f5; padding: 15px; margin: 20px 0; }");
        html.append(".section { margin: 20px 0; }");
        html.append("</style></head><body>");

        html.append("<h1>Diagnostic Report</h1>");

        html.append("<div class='meta'>");
        html.append("<p><strong>Report ID:</strong> ").append(report.getId()).append("</p>");
        html.append("<p><strong>Date:</strong> ").append(report.getCreatedAt()).append("</p>");
        if (report.getAuthor() != null) {
            html.append("<p><strong>Author:</strong> ").append(report.getAuthor().getFullName()).append("</p>");
        }
        html.append("<p><strong>Status:</strong> ").append(report.getFinalized() ? "Final" : "Draft").append("</p>");
        html.append("</div>");

        if (report.getFindings() != null && !report.getFindings().isEmpty()) {
            html.append("<div class='section'><h2>Findings</h2>");
            html.append("<p>").append(report.getFindings().replace("\n", "<br/>")).append("</p></div>");
        }

        if (report.getImpression() != null && !report.getImpression().isEmpty()) {
            html.append("<div class='section'><h2>Impression</h2>");
            html.append("<p>").append(report.getImpression().replace("\n", "<br/>")).append("</p></div>");
        }

        if (report.getRecommendations() != null && !report.getRecommendations().isEmpty()) {
            html.append("<div class='section'><h2>Recommendations</h2>");
            html.append("<p>").append(report.getRecommendations().replace("\n", "<br/>")).append("</p></div>");
        }

        html.append("<hr/><p style='color:#999;font-size:12px;'>Generated by MedAI Platform</p>");
        html.append("</body></html>");

        byte[] htmlBytes = html.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDispositionFormData("attachment", "report_" + id + ".html");
        headers.setContentLength(htmlBytes.length);

        auditService.logAnonymous("EXPORT_PDF", "REPORT", id.toString());

        return new ResponseEntity<>(htmlBytes, headers, HttpStatus.OK);
    }
}