// file: src/main/java/com/team/medaibackend/service/ReportService.java
package com.team.medaibackend.service;

import com.team.medaibackend.dto.CreateReportRequest;
import com.team.medaibackend.dto.ReportDto;
import com.team.medaibackend.dto.UpdateReportRequest;
import com.team.medaibackend.entity.Report;
import com.team.medaibackend.entity.Study;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.ReportRepository;
import com.team.medaibackend.repository.StudyRepository;
import com.team.medaibackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ReportService(ReportRepository reportRepository,
                         StudyRepository studyRepository,
                         UserRepository userRepository,
                         AuditService auditService) {
        this.reportRepository = reportRepository;
        this.studyRepository = studyRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReportDto createReport(CreateReportRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found: " + authorId));

        // Handle null studyId - allow reports without studies
        Study study = null;
        if (request.getStudyId() != null) {
            study = studyRepository.findById(request.getStudyId())
                    .orElseThrow(() -> new RuntimeException("Study not found: " + request.getStudyId()));
        }

        Report report = new Report();
        report.setReportUid(generateReportUid());
        report.setStudy(study);
        report.setAuthor(author);
        report.setTitle(request.getTitle());
        report.setSummary(request.getSummary());
        report.setFindings(request.getFindings());
        report.setImpression(request.getImpression());
        report.setRecommendations(request.getRecommendations());
        report.setContent(request.getContent());
        report.setStatus("draft");
        report.setFinalized(false);

        Report saved = reportRepository.save(report);

        auditService.log(AuditService.ACTION_CREATE, "REPORT", saved.getReportUid(),
                authorId, author.getUsername());

        return toDto(saved);
    }

    @Transactional
    public ReportDto updateReport(Long reportId, UpdateReportRequest request, Long userId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getFinalized()) {
            throw new RuntimeException("Cannot modify finalized report");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (request.getTitle() != null) report.setTitle(request.getTitle());
        if (request.getSummary() != null) report.setSummary(request.getSummary());
        if (request.getFindings() != null) report.setFindings(request.getFindings());
        if (request.getImpression() != null) report.setImpression(request.getImpression());
        if (request.getRecommendations() != null) report.setRecommendations(request.getRecommendations());
        if (request.getContent() != null) report.setContent(request.getContent());
        if (request.getStatus() != null) report.setStatus(request.getStatus());

        Report saved = reportRepository.save(report);

        auditService.log(AuditService.ACTION_UPDATE, "REPORT", saved.getReportUid(),
                userId, user.getUsername());

        return toDto(saved);
    }

    @Transactional
    public ReportDto finalizeReport(Long reportId, Long userId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getFinalized()) {
            throw new RuntimeException("Report is already finalized");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        report.setFinalized(true);
        report.setFinalizedAt(LocalDateTime.now());
        report.setStatus("finalized");

        Report saved = reportRepository.save(report);

        auditService.log(AuditService.ACTION_FINALIZE, "REPORT", saved.getReportUid(),
                userId, user.getUsername());

        return toDto(saved);
    }

    @Transactional
    public void deleteReport(Long reportId, Long userId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getFinalized()) {
            throw new RuntimeException("Cannot delete finalized report");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String reportUid = report.getReportUid();
        reportRepository.delete(report);

        auditService.log(AuditService.ACTION_DELETE, "REPORT", reportUid,
                userId, user.getUsername());
    }

    @Transactional(readOnly = true)
    public ReportDto getReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
        return toDto(report);
    }

    @Transactional(readOnly = true)
    public ReportDto getReportByUid(String reportUid) {
        Report report = reportRepository.findByReportUid(reportUid)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportUid));
        return toDto(report);
    }

    @Transactional(readOnly = true)
    public List<ReportDto> getReportsByStudy(Long studyId) {
        return reportRepository.findByStudyId(studyId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReportDto> getAllReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportRepository.findAll(pageable).map(this::toDto);
    }

    // ✅ NEW: used by /api/reports/patient
    @Transactional(readOnly = true)
    public List<ReportDto> getReportsByPatientDbId(Long patientDbId) {
        return reportRepository.findByPatientId(patientDbId).stream()
                .map(this::toDto)
                .toList();
    }

    private String generateReportUid() {
        return "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ReportDto toDto(Report report) {
        ReportDto dto = new ReportDto();
        dto.setId(report.getId());
        dto.setReportUid(report.getReportUid());

        // Handle null study (for general reports)
        if (report.getStudy() != null) {
            dto.setStudyId(report.getStudy().getId());
            dto.setStudyUid(report.getStudy().getStudyUid());
            if (report.getStudy().getPatient() != null) {
                dto.setPatientId(report.getStudy().getPatient().getPatientId());
                dto.setPatientName(report.getStudy().getPatient().getName());
            }
        }

        dto.setAuthorId(report.getAuthor().getId());
        dto.setAuthorName(report.getAuthor().getFullName() != null ?
                report.getAuthor().getFullName() : report.getAuthor().getUsername());
        dto.setTitle(report.getTitle());
        dto.setSummary(report.getSummary());
        dto.setFindings(report.getFindings());
        dto.setImpression(report.getImpression());
        dto.setRecommendations(report.getRecommendations());
        dto.setContent(report.getContent());
        dto.setStatus(report.getStatus());
        dto.setFinalized(report.getFinalized());
        dto.setFinalizedAt(report.getFinalizedAt());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        return dto;
    }
}
