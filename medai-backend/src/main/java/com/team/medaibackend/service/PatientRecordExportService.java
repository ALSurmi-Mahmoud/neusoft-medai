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
import java.util.Map;

@Service
public class PatientRecordExportService {

    private final PatientRepository patientRepository;
    private final ReportTemplateRepository templateRepository;
    private final ExportedReportRepository exportedReportRepository;
    private final PdfExportService pdfExportService;
    private final DocxExportService docxExportService;
    private final FileStorage fileStorage;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public PatientRecordExportService(
            PatientRepository patientRepository,
            ReportTemplateRepository templateRepository,
            ExportedReportRepository exportedReportRepository,
            PdfExportService pdfExportService,
            DocxExportService docxExportService,
            FileStorage fileStorage,
            SecurityUtils securityUtils,
            AuditService auditService) {
        this.patientRepository = patientRepository;
        this.templateRepository = templateRepository;
        this.exportedReportRepository = exportedReportRepository;
        this.pdfExportService = pdfExportService;
        this.docxExportService = docxExportService;
        this.fileStorage = fileStorage;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional
    public ExportedReport exportPatientSummary(Long patientId, String format) throws Exception {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        ReportTemplate template = templateRepository
                .findByCategoryAndIsActiveTrueOrderByUsageCountDesc("patient_summary")
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No patient summary template found"));

        Map<String, String> variables = buildPatientSummaryVariables(patient);

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
        String patientName = patient.getName().replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = String.format("PatientSummary_%s_%s.%s", patientName, timestamp, fileExtension);

        String filePath = fileStorage.store(fileContent, fileName, "reports");

        ExportedReport exportedReport = new ExportedReport();
        exportedReport.setPatient(patient);
        exportedReport.setExportType("patient_record");
        exportedReport.setFormat(format);
        exportedReport.setTemplate(template);
        exportedReport.setFileName(fileName);
        exportedReport.setFilePath(filePath);
        exportedReport.setFileSize((long) fileContent.length);
        exportedReport.setTitle("Patient Summary - " + patient.getName());
        exportedReport.setExportedBy(currentUser);

        ExportedReport saved = exportedReportRepository.save(exportedReport);

        auditService.log(
                AuditService.ACTION_EXPORT,
                "PATIENT",
                patient.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    private Map<String, String> buildPatientSummaryVariables(Patient patient) {
        Map<String, String> vars = new HashMap<>();
        vars.put("hospital_name", "Medical Center");
        vars.put("patient_name", patient.getName() != null ? patient.getName() : "");
        vars.put("patient_id", patient.getPatientId() != null ? patient.getPatientId() : "");
        vars.put("patient_dob", patient.getBirthDate() != null ? patient.getBirthDate().toString() : "");
        vars.put("patient_sex", patient.getSex() != null ? patient.getSex() : "");
        vars.put("patient_blood_type", patient.getBloodType() != null ? patient.getBloodType() : "N/A");
        vars.put("patient_phone", patient.getPhone() != null ? patient.getPhone() : "");
        vars.put("patient_email", patient.getEmail() != null ? patient.getEmail() : "");
        vars.put("patient_address", patient.getAddress() != null ? patient.getAddress() : "");
        vars.put("patient_city", patient.getCity() != null ? patient.getCity() : "");
        vars.put("patient_state", patient.getState() != null ? patient.getState() : "");
        vars.put("patient_zip", patient.getZipCode() != null ? patient.getZipCode() : "");
        vars.put("patient_country", patient.getCountry() != null ? patient.getCountry() : "");
        vars.put("emergency_contact_name", patient.getEmergencyContactName() != null ?
                patient.getEmergencyContactName() : "");
        vars.put("emergency_contact_phone", patient.getEmergencyContactPhone() != null ?
                patient.getEmergencyContactPhone() : "");
        vars.put("emergency_contact_relationship", patient.getEmergencyContactRelationship() != null ?
                patient.getEmergencyContactRelationship() : "");
        vars.put("patient_allergies", patient.getAllergies() != null ? patient.getAllergies() : "None");
        vars.put("patient_conditions", patient.getMedicalConditions() != null ?
                patient.getMedicalConditions() : "None");
        vars.put("patient_medications", patient.getCurrentMedications() != null ?
                patient.getCurrentMedications() : "None");
        vars.put("insurance_provider", patient.getInsuranceProvider() != null ?
                patient.getInsuranceProvider() : "");
        vars.put("insurance_policy", patient.getInsurancePolicyNumber() != null ?
                patient.getInsurancePolicyNumber() : "");
        vars.put("report_date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return vars;
    }
}