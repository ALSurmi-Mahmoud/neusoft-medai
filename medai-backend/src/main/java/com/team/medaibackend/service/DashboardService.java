package com.team.medaibackend.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Dashboard aggregation service
 * Combines data from multiple analytics services
 */
@Service
public class DashboardService {

    private final AnalyticsService analyticsService;
    private final PatientAnalyticsService patientAnalyticsService;
    private final StudyAnalyticsService studyAnalyticsService;
    private final ReportAnalyticsService reportAnalyticsService;
    private final TreatmentAnalyticsService treatmentAnalyticsService;
    private final UserAnalyticsService userAnalyticsService;
    private final SystemAnalyticsService systemAnalyticsService;

    public DashboardService(
            AnalyticsService analyticsService,
            PatientAnalyticsService patientAnalyticsService,
            StudyAnalyticsService studyAnalyticsService,
            ReportAnalyticsService reportAnalyticsService,
            TreatmentAnalyticsService treatmentAnalyticsService,
            UserAnalyticsService userAnalyticsService,
            SystemAnalyticsService systemAnalyticsService) {
        this.analyticsService = analyticsService;
        this.patientAnalyticsService = patientAnalyticsService;
        this.studyAnalyticsService = studyAnalyticsService;
        this.reportAnalyticsService = reportAnalyticsService;
        this.treatmentAnalyticsService = treatmentAnalyticsService;
        this.userAnalyticsService = userAnalyticsService;
        this.systemAnalyticsService = systemAnalyticsService;
    }

    /**
     * Get complete dashboard data
     * Cached for 5 minutes
     */
    @Cacheable(value = "dashboard", key = "'overview'")
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> dashboard = new HashMap<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        // System overview
        dashboard.put("overview", analyticsService.getSystemOverview());

        // KPIs
        dashboard.put("kpis", analyticsService.getKPIs(startDate, endDate));

        // Recent activity
        dashboard.put("recentActivity", analyticsService.getRecentActivity(10));

        // Quick stats
        Map<String, Object> quickStats = new HashMap<>();
        quickStats.put("studies", studyAnalyticsService.getPerformanceMetrics());
        quickStats.put("reports", reportAnalyticsService.getStatusDistribution());
        quickStats.put("treatments", treatmentAnalyticsService.getOverview());
        dashboard.put("quickStats", quickStats);

        return dashboard;
    }

    /**
     * Get dashboard widgets
     */
    @Cacheable(value = "dashboard", key = "'widgets'")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWidgets() {
        List<Map<String, Object>> widgets = new ArrayList<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        // Patient demographics widget
        Map<String, Object> patientWidget = new HashMap<>();
        patientWidget.put("id", "patient-demographics");
        patientWidget.put("title", "Patient Demographics");
        patientWidget.put("type", "chart");
        patientWidget.put("data", patientAnalyticsService.getDemographics());
        widgets.add(patientWidget);

        // Study trends widget
        Map<String, Object> studyWidget = new HashMap<>();
        studyWidget.put("id", "study-trends");
        studyWidget.put("title", "Study Volume");
        studyWidget.put("type", "line-chart");
        studyWidget.put("data", studyAnalyticsService.getStudyTrends(startDate, endDate));
        widgets.add(studyWidget);

        // Report status widget
        Map<String, Object> reportWidget = new HashMap<>();
        reportWidget.put("id", "report-status");
        reportWidget.put("title", "Report Status");
        reportWidget.put("type", "pie-chart");
        reportWidget.put("data", reportAnalyticsService.getStatusDistribution());
        widgets.add(reportWidget);

        return widgets;
    }
}