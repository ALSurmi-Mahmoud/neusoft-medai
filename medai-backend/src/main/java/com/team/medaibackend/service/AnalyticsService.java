package com.team.medaibackend.service;

import com.team.medaibackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Core analytics service providing system-wide metrics
 */
@Service
public class AnalyticsService {

    private final PatientRepository patientRepository;
    private final StudyRepository studyRepository;
    private final ReportRepository reportRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AnalyticsService(
            PatientRepository patientRepository,
            StudyRepository studyRepository,
            ReportRepository reportRepository,
            TreatmentPlanRepository treatmentPlanRepository,
            ClinicalNoteRepository clinicalNoteRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        this.patientRepository = patientRepository;
        this.studyRepository = studyRepository;
        this.reportRepository = reportRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Get system overview with key counts
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        overview.put("totalPatients", patientRepository.count());
        overview.put("totalStudies", studyRepository.count());
        overview.put("totalReports", reportRepository.count());
        overview.put("activeTreatmentPlans",
                treatmentPlanRepository.countByStatus("active"));
        overview.put("totalUsers", userRepository.count());

        return overview;
    }

    /**
     * Get KPIs with trends
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getKPIs(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> kpis = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Total patients with trend
        long totalPatients = patientRepository.count();
        long newPatients = patientRepository.countByCreatedAtBetween(start, end);

        Map<String, Object> patientKPI = new HashMap<>();
        patientKPI.put("total", totalPatients);
        patientKPI.put("new", newPatients);
        patientKPI.put("trend", calculateTrend(totalPatients, newPatients));
        kpis.put("patients", patientKPI);

        // Active studies
        long activeStudies = studyRepository.countByStatus("uploaded");
        long totalStudies = studyRepository.count();

        Map<String, Object> studyKPI = new HashMap<>();
        studyKPI.put("active", activeStudies);
        studyKPI.put("total", totalStudies);
        studyKPI.put("trend", 0); // Calculate based on historical data
        kpis.put("studies", studyKPI);

        // Reports this period
        long reportsInPeriod = reportRepository.countByCreatedAtBetween(start, end);
        long finalizedReports = reportRepository.countByStatusAndCreatedAtBetween(
                "finalized", start, end);

        Map<String, Object> reportKPI = new HashMap<>();
        reportKPI.put("total", reportsInPeriod);
        reportKPI.put("finalized", finalizedReports);
        reportKPI.put("trend", 0);
        kpis.put("reports", reportKPI);

        // Active treatment plans
        long activePlans = treatmentPlanRepository.countByStatus("active");
        long completedPlans = treatmentPlanRepository.countByStatus("completed");

        Map<String, Object> planKPI = new HashMap<>();
        planKPI.put("active", activePlans);
        planKPI.put("completed", completedPlans);
        planKPI.put("successRate", calculateSuccessRate(completedPlans, activePlans));
        kpis.put("treatmentPlans", planKPI);

        return kpis;
    }

    /**
     * Get recent activity feed
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentActivity(int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();

        // Get recent audit logs
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        var auditLogs = auditLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);

        auditLogs.stream()
                .limit(limit)
                .forEach(log -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("timestamp", log.getCreatedAt());
                    activity.put("username", log.getUsername());
                    activity.put("action", log.getAction());
                    activity.put("resourceType", log.getResourceType());
                    activity.put("resourceId", log.getResourceId());
                    activities.add(activity);
                });

        return activities;
    }

    /**
     * Get data for date range comparison
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComparison(
            LocalDate currentStart, LocalDate currentEnd,
            LocalDate previousStart, LocalDate previousEnd) {

        Map<String, Object> comparison = new HashMap<>();

        LocalDateTime currStart = currentStart.atStartOfDay();
        LocalDateTime currEnd = currentEnd.atTime(23, 59, 59);
        LocalDateTime prevStart = previousStart.atStartOfDay();
        LocalDateTime prevEnd = previousEnd.atTime(23, 59, 59);

        // Compare patients
        long currentPatients = patientRepository.countByCreatedAtBetween(currStart, currEnd);
        long previousPatients = patientRepository.countByCreatedAtBetween(prevStart, prevEnd);

        Map<String, Object> patientComp = new HashMap<>();
        patientComp.put("current", currentPatients);
        patientComp.put("previous", previousPatients);
        patientComp.put("change", currentPatients - previousPatients);
        patientComp.put("percentChange", calculatePercentChange(previousPatients, currentPatients));
        comparison.put("patients", patientComp);

        // Compare studies
        long currentStudies = studyRepository.countByCreatedAtBetween(currStart, currEnd);
        long previousStudies = studyRepository.countByCreatedAtBetween(prevStart, prevEnd);

        Map<String, Object> studyComp = new HashMap<>();
        studyComp.put("current", currentStudies);
        studyComp.put("previous", previousStudies);
        studyComp.put("change", currentStudies - previousStudies);
        studyComp.put("percentChange", calculatePercentChange(previousStudies, currentStudies));
        comparison.put("studies", studyComp);

        // Compare reports
        long currentReports = reportRepository.countByCreatedAtBetween(currStart, currEnd);
        long previousReports = reportRepository.countByCreatedAtBetween(prevStart, prevEnd);

        Map<String, Object> reportComp = new HashMap<>();
        reportComp.put("current", currentReports);
        reportComp.put("previous", previousReports);
        reportComp.put("change", currentReports - previousReports);
        reportComp.put("percentChange", calculatePercentChange(previousReports, currentReports));
        comparison.put("reports", reportComp);

        return comparison;
    }

    // Helper methods
    private double calculateTrend(long total, long newItems) {
        if (total == 0) return 0;
        return ((double) newItems / total) * 100;
    }

    private double calculateSuccessRate(long completed, long active) {
        long total = completed + active;
        if (total == 0) return 0;
        return ((double) completed / total) * 100;
    }

    private double calculatePercentChange(long previous, long current) {
        if (previous == 0) return current > 0 ? 100 : 0;
        return ((double) (current - previous) / previous) * 100;
    }
}