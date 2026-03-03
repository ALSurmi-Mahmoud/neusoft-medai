package com.team.medaibackend.web;

import com.team.medaibackend.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final PatientAnalyticsService patientAnalytics;
    private final StudyAnalyticsService studyAnalytics;
    private final ReportAnalyticsService reportAnalytics;
    private final TreatmentAnalyticsService treatmentAnalytics;
    private final UserAnalyticsService userAnalytics;
    private final SystemAnalyticsService systemAnalytics;

    public AnalyticsController(
            PatientAnalyticsService patientAnalytics,
            StudyAnalyticsService studyAnalytics,
            ReportAnalyticsService reportAnalytics,
            TreatmentAnalyticsService treatmentAnalytics,
            UserAnalyticsService userAnalytics,
            SystemAnalyticsService systemAnalytics) {
        this.patientAnalytics = patientAnalytics;
        this.studyAnalytics = studyAnalytics;
        this.reportAnalytics = reportAnalytics;
        this.treatmentAnalytics = treatmentAnalytics;
        this.userAnalytics = userAnalytics;
        this.systemAnalytics = systemAnalytics;
    }

    // Patient Analytics
    @GetMapping("/patients/demographics")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getPatientDemographics() {
        return ResponseEntity.ok(patientAnalytics.getDemographics());
    }

    @GetMapping("/patients/trends")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getPatientTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(patientAnalytics.getPatientTrends(startDate, endDate));
    }

    @GetMapping("/patients/conditions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getCommonConditions() {
        return ResponseEntity.ok(patientAnalytics.getCommonConditions());
    }

    @GetMapping("/patients/allergies")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getCommonAllergies() {
        return ResponseEntity.ok(patientAnalytics.getCommonAllergies());
    }

    @GetMapping("/patients/statistics")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getPatientStatistics() {
        return ResponseEntity.ok(patientAnalytics.getPatientStatistics());
    }

    // Study Analytics
    @GetMapping("/studies/by-modality")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getStudiesByModality() {
        return ResponseEntity.ok(studyAnalytics.getStudiesByModality());
    }

    @GetMapping("/studies/trends")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getStudyTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(studyAnalytics.getStudyTrends(startDate, endDate));
    }

    @GetMapping("/studies/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getStudyStatus() {
        return ResponseEntity.ok(studyAnalytics.getStudyStatusDistribution());
    }

    @GetMapping("/studies/performance")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getStudyPerformance() {
        return ResponseEntity.ok(studyAnalytics.getPerformanceMetrics());
    }

    @GetMapping("/studies/heatmap")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getStudyHeatmap() {
        return ResponseEntity.ok(studyAnalytics.getHourlyActivity());
    }

    // Report Analytics
    @GetMapping("/reports/production")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getReportProduction(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportAnalytics.getProductionMetrics(startDate, endDate));
    }

    @GetMapping("/reports/trends")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getReportTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportAnalytics.getReportTrends(startDate, endDate));
    }

    @GetMapping("/reports/by-doctor")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getReportsByDoctor(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportAnalytics.getReportsByDoctor(startDate, endDate));
    }

    @GetMapping("/reports/quality")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getReportQuality() {
        return ResponseEntity.ok(reportAnalytics.getQualityMetrics());
    }

    @GetMapping("/reports/turnaround")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getTurnaround() {
        return ResponseEntity.ok(reportAnalytics.getTurnaroundAnalysis());
    }

    // Treatment Analytics
    @GetMapping("/treatments/overview")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getTreatmentOverview() {
        return ResponseEntity.ok(treatmentAnalytics.getOverview());
    }

    @GetMapping("/treatments/by-category")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getTreatmentsByCategory() {
        return ResponseEntity.ok(treatmentAnalytics.getPlansByCategory());
    }

    @GetMapping("/treatments/by-priority")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getTreatmentsByPriority() {
        return ResponseEntity.ok(treatmentAnalytics.getPlansByPriority());
    }

    @GetMapping("/treatments/success-rate")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getSuccessRate() {
        return ResponseEntity.ok(treatmentAnalytics.getSuccessRateAnalysis());
    }

    @GetMapping("/treatments/duration")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getDuration() {
        return ResponseEntity.ok(treatmentAnalytics.getDurationAnalysis());
    }

    // User Analytics
    @GetMapping("/users/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserActivity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(userAnalytics.getActivityMetrics(startDate, endDate));
    }

    @GetMapping("/users/most-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMostActiveUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userAnalytics.getMostActiveUsers(startDate, endDate, limit));
    }

    @GetMapping("/users/actions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getActionDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(userAnalytics.getActionDistribution(startDate, endDate));
    }

    @GetMapping("/users/heatmap")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserHeatmap() {
        return ResponseEntity.ok(userAnalytics.getActivityHeatmap());
    }

    // System Analytics
    @GetMapping("/system/performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSystemPerformance() {
        return ResponseEntity.ok(systemAnalytics.getPerformanceMetrics());
    }

    @GetMapping("/system/storage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStorageMetrics() {
        return ResponseEntity.ok(systemAnalytics.getStorageMetrics());
    }

    @GetMapping("/system/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSystemHealth() {
        return ResponseEntity.ok(systemAnalytics.getHealthStatus());
    }
}