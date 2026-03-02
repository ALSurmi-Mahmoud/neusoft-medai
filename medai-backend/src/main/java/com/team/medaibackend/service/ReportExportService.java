package com.team.medaibackend.service;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import com.team.medaibackend.security.SecurityUtils;
import com.team.medaibackend.storage.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportExportService {

    private final ReportRepository reportRepository;
    private final ReportTemplateRepository templateRepository;
    private final ExportedReportRepository exportedReportRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final PdfExportService pdfExportService;
    private final DocxExportService docxExportService;
    private final FileStorage fileStorage;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public ReportExportService(
            ReportRepository reportRepository,
            ReportTemplateRepository templateRepository,
            ExportedReportRepository exportedReportRepository,
            TreatmentPlanRepository treatmentPlanRepository,
            ClinicalNoteRepository clinicalNoteRepository,
            PdfExportService pdfExportService,
            DocxExportService docxExportService,
            FileStorage fileStorage,
            SecurityUtils securityUtils,
            AuditService auditService) {
        this.reportRepository = reportRepository;
        this.templateRepository = templateRepository;
        this.exportedReportRepository = exportedReportRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.pdfExportService = pdfExportService;
        this.docxExportService = docxExportService;
        this.fileStorage = fileStorage;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional
    public ExportedReport exportReport(Long reportId, String format, Long templateId) throws Exception {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        if (!format.equals("pdf") && !format.equals("docx")) {
            throw new IllegalArgumentException("Invalid format. Must be 'pdf' or 'docx'");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        ReportTemplate template;
        if (templateId != null) {
            template = templateRepository.findById(templateId).orElseThrow();
        } else {
            template = templateRepository.findByCategoryAndIsActiveTrueOrderByUsageCountDesc("radiology")
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No radiology template found"));
        }

        Map<String, String> variables = buildReportVariables(report);

        byte[] fileContent;
        String fileExtension;

        if ("pdf".equals(format)) {
            String html = pdfExportService.replacePlaceholders(template.getContent(), variables);
            html = pdfExportService.wrapHtmlDocument(html);
            fileContent = pdfExportService.generatePdfFromHtml(html);
            fileExtension = "pdf";
        } else {
            String html = pdfExportService.replacePlaceholders(template.getContent(), variables);
            fileContent = docxExportService.generateDocxFromHtml(html);
            fileExtension = "docx";
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String patientName = report.getPatient() != null ?
                report.getPatient().getName().replaceAll("[^a-zA-Z0-9]", "_") : "Unknown";
        String fileName = String.format("Report_%s_%s.%s", patientName, timestamp, fileExtension);

        String filePath = fileStorage.store(fileContent, fileName, "reports");

        ExportedReport exportedReport = new ExportedReport();
        exportedReport.setReport(report);
        exportedReport.setPatient(report.getPatient());
        exportedReport.setExportType("single_report");
        exportedReport.setFormat(format);
        exportedReport.setTemplate(template);
        exportedReport.setFileName(fileName);
        exportedReport.setFilePath(filePath);
        exportedReport.setFileSize((long) fileContent.length);
        exportedReport.setTitle(report.getTitle());
        exportedReport.setExportedBy(currentUser);

        ExportedReport saved = exportedReportRepository.save(exportedReport);

        auditService.log(
                AuditService.ACTION_EXPORT,
                "REPORT",
                report.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    @Transactional
    public ExportedReport exportTreatmentPlan(Long planId, String format, Long templateId) throws Exception {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        TreatmentPlan plan = treatmentPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Treatment plan not found: " + planId));

        ReportTemplate template;
        if (templateId != null) {
            template = templateRepository.findById(templateId).orElseThrow();
        } else {
            template = templateRepository.findByCategoryAndIsActiveTrueOrderByUsageCountDesc("treatment_plan")
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No treatment plan template found"));
        }

        Map<String, String> variables = buildTreatmentPlanVariables(plan);

        byte[] fileContent;
        String fileExtension;

        if ("pdf".equals(format)) {
            String html = pdfExportService.replacePlaceholders(template.getContent(), variables);
            html = pdfExportService.wrapHtmlDocument(html);
            fileContent = pdfExportService.generatePdfFromHtml(html);
            fileExtension = "pdf";
        } else {
            String html = pdfExportService.replacePlaceholders(template.getContent(), variables);
            fileContent = docxExportService.generateDocxFromHtml(html);
            fileExtension = "docx";
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String patientName = plan.getPatient().getName().replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = String.format("TreatmentPlan_%s_%s.%s", patientName, timestamp, fileExtension);

        String filePath = fileStorage.store(fileContent, fileName, "reports");

        ExportedReport exportedReport = new ExportedReport();
        exportedReport.setTreatmentPlan(plan);
        exportedReport.setPatient(plan.getPatient());
        exportedReport.setExportType("treatment_plan");
        exportedReport.setFormat(format);
        exportedReport.setTemplate(template);
        exportedReport.setFileName(fileName);
        exportedReport.setFilePath(filePath);
        exportedReport.setFileSize((long) fileContent.length);
        exportedReport.setTitle(plan.getTitle());
        exportedReport.setExportedBy(currentUser);

        ExportedReport saved = exportedReportRepository.save(exportedReport);

        auditService.log(
                AuditService.ACTION_EXPORT,
                "TREATMENT_PLAN",
                plan.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    @Transactional
    public ExportedReport exportClinicalNote(Long noteId, String format, Long templateId) throws Exception {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));

        ReportTemplate template;
        if (templateId != null) {
            template = templateRepository.findById(templateId).orElseThrow();
        } else {
            template = templateRepository.findByCategoryAndIsActiveTrueOrderByUsageCountDesc("clinical_note")
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No clinical note template found"));
        }

        Map<String, String> variables = buildClinicalNoteVariables(note);

        byte[] fileContent;
        String fileExtension;

        if ("pdf".equals(format)) {
            String html = pdfExportService.replacePlaceholders(template.getContent(), variables);
            html = pdfExportService.wrapHtmlDocument(html);
            fileContent = pdfExportService.generatePdfFromHtml(html);
            fileExtension = "pdf";
        } else {
            String html = pdfExportService.replacePlaceholders(template.getContent(), variables);
            fileContent = docxExportService.generateDocxFromHtml(html);
            fileExtension = "docx";
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String patientName = note.getPatient().getName().replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = String.format("ClinicalNote_%s_%s.%s", patientName, timestamp, fileExtension);

        String filePath = fileStorage.store(fileContent, fileName, "reports");

        ExportedReport exportedReport = new ExportedReport();
        exportedReport.setClinicalNote(note);
        exportedReport.setPatient(note.getPatient());
        exportedReport.setExportType("clinical_note");
        exportedReport.setFormat(format);
        exportedReport.setTemplate(template);
        exportedReport.setFileName(fileName);
        exportedReport.setFilePath(filePath);
        exportedReport.setFileSize((long) fileContent.length);
        exportedReport.setTitle(note.getTitle());
        exportedReport.setExportedBy(currentUser);

        ExportedReport saved = exportedReportRepository.save(exportedReport);

        auditService.log(
                AuditService.ACTION_EXPORT,
                "CLINICAL_NOTE",
                note.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    @Transactional
    public byte[] downloadExport(Long exportId) throws Exception {
        ExportedReport export = exportedReportRepository.findById(exportId)
                .orElseThrow(() -> new RuntimeException("Export not found: " + exportId));

        exportedReportRepository.incrementDownloadCount(exportId, LocalDateTime.now());

        return fileStorage.loadAsBytes(export.getFilePath());
    }

    @Transactional(readOnly = true)
    public List<ExportedReport> getUserExportHistory() {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        return exportedReportRepository.findUserRecentExports(currentUser.getId(), oneMonthAgo);
    }

    private Map<String, String> buildReportVariables(Report report) {
        Map<String, String> vars = new HashMap<>();
        vars.put("hospital_name", "Medical Imaging Center");

        if (report.getPatient() != null) {
            Patient patient = report.getPatient();
            vars.put("patient_name", patient.getName() != null ? patient.getName() : "");
            vars.put("patient_id", patient.getPatientId() != null ? patient.getPatientId() : "");
            vars.put("patient_dob", patient.getBirthDate() != null ? patient.getBirthDate().toString() : "");
            vars.put("patient_sex", patient.getSex() != null ? patient.getSex() : "");
        }

        if (report.getStudy() != null) {
            Study study = report.getStudy();
            vars.put("study_date", study.getStudyDate() != null ?
                    study.getStudyDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            vars.put("study_modality", study.getModality() != null ? study.getModality() : "");
            vars.put("study_accession", study.getAccessionNumber() != null ? study.getAccessionNumber() : "");
            vars.put("study_description", study.getDescription() != null ? study.getDescription() : "");
        }

        vars.put("report_findings", report.getFindings() != null ? report.getFindings() : "");
        vars.put("report_impression", report.getImpression() != null ? report.getImpression() : "");
        vars.put("report_recommendations", report.getRecommendations() != null ? report.getRecommendations() : "");

        if (report.getAuthor() != null) {
            vars.put("author_name", report.getAuthor().getFullName() != null ?
                    report.getAuthor().getFullName() : "");
        }

        vars.put("report_date", report.getCreatedAt() != null ?
                report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");

        return vars;
    }

    private Map<String, String> buildTreatmentPlanVariables(TreatmentPlan plan) {
        Map<String, String> vars = new HashMap<>();
        vars.put("hospital_name", "Medical Center");
        vars.put("patient_name", plan.getPatient().getName());
        vars.put("patient_id", plan.getPatient().getPatientId());
        vars.put("plan_title", plan.getTitle() != null ? plan.getTitle() : "");
        vars.put("plan_diagnosis", plan.getDiagnosis() != null ? plan.getDiagnosis() : "");
        vars.put("plan_goals", plan.getGoals() != null ? plan.getGoals() : "");
        vars.put("plan_description", plan.getDescription() != null ? plan.getDescription() : "");
        vars.put("plan_notes", plan.getNotes() != null ? plan.getNotes() : "");
        vars.put("plan_start_date", plan.getStartDate() != null ? plan.getStartDate().toString() : "");
        vars.put("plan_end_date", plan.getEndDate() != null ? plan.getEndDate().toString() : "");
        vars.put("plan_status", plan.getStatus() != null ? plan.getStatus() : "");
        vars.put("plan_priority", plan.getPriority() != null ? plan.getPriority() : "");
        vars.put("plan_progress", plan.getProgressPercentage() != null ?
                plan.getProgressPercentage().toString() : "0");

        if (plan.getDoctor() != null) {
            vars.put("doctor_name", plan.getDoctor().getFullName());
        }

        return vars;
    }

    private Map<String, String> buildClinicalNoteVariables(ClinicalNote note) {
        Map<String, String> vars = new HashMap<>();
        vars.put("hospital_name", "Medical Center");
        vars.put("patient_name", note.getPatient().getName());
        vars.put("patient_id", note.getPatient().getPatientId());
        vars.put("patient_dob", note.getPatient().getBirthDate() != null ?
                note.getPatient().getBirthDate().toString() : "");
        vars.put("visit_date", note.getNoteDate() != null ? note.getNoteDate().toString() : "");
        vars.put("note_date", note.getNoteDate() != null ? note.getNoteDate().toString() : "");
        vars.put("note_subjective", note.getSubjective() != null ? note.getSubjective() : "");
        vars.put("note_objective", note.getObjective() != null ? note.getObjective() : "");
        vars.put("note_assessment", note.getAssessment() != null ? note.getAssessment() : "");
        vars.put("note_plan", note.getPlan() != null ? note.getPlan() : "");

        if (note.getVitals() != null && !note.getVitals().isEmpty()) {
            String vitalsTable = pdfExportService.formatVitalsTable(note.getVitals());
            vars.put("vitals_table", vitalsTable);
        } else {
            vars.put("vitals_table", "<p>No vitals recorded.</p>");
        }

        if (note.getDoctor() != null) {
            vars.put("doctor_name", note.getDoctor().getFullName());
        }

        return vars;
    }
}