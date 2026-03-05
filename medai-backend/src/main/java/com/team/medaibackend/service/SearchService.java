package com.team.medaibackend.service;

import com.team.medaibackend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Core search service - orchestrates searches across all entities
 */
@Service
public class SearchService {

    private final PatientSearchService patientSearchService;
    private final StudySearchService studySearchService;
    private final ReportSearchService reportSearchService;
    private final TreatmentSearchService treatmentSearchService;
    private final SecurityUtils securityUtils;

    public SearchService(
            PatientSearchService patientSearchService,
            StudySearchService studySearchService,
            ReportSearchService reportSearchService,
            TreatmentSearchService treatmentSearchService,
            SecurityUtils securityUtils) {
        this.patientSearchService = patientSearchService;
        this.studySearchService = studySearchService;
        this.reportSearchService = reportSearchService;
        this.treatmentSearchService = treatmentSearchService;
        this.securityUtils = securityUtils;
    }

    /**
     * Global search across all entities
     */
    @Transactional(readOnly = true)
    public Map<String, Object> globalSearch(String query, int limit) {
        Map<String, Object> results = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String searchQuery = query.trim();

        // Search all entity types in parallel (simplified - sequential for now)
        results.put("patients", patientSearchService.searchPatients(searchQuery, limit));
        results.put("studies", studySearchService.searchStudies(searchQuery, limit));
        results.put("reports", reportSearchService.searchReports(searchQuery, limit));
        results.put("treatments", treatmentSearchService.searchTreatmentPlans(searchQuery, limit));

        // Calculate total count
        int totalCount = 0;
        for (Object value : results.values()) {
            if (value instanceof List) {
                totalCount += ((List<?>) value).size();
            }
        }
        results.put("totalCount", totalCount);

        return results;
    }

    /**
     * Get search suggestions
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSearchSuggestions(String query, int limit) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        if (query == null || query.trim().isEmpty() || query.length() < 2) {
            return suggestions;
        }

        String searchQuery = query.trim();

        // Get top suggestions from each entity type
        int perType = Math.max(2, limit / 4);

        List<Map<String, Object>> patients = patientSearchService.searchPatients(searchQuery, perType);
        List<Map<String, Object>> studies = studySearchService.searchStudies(searchQuery, perType);
        List<Map<String, Object>> reports = reportSearchService.searchReports(searchQuery, perType);
        List<Map<String, Object>> treatments = treatmentSearchService.searchTreatmentPlans(searchQuery, perType);

        // Combine and add type labels
        patients.forEach(p -> {
            p.put("type", "patient");
            p.put("icon", "User");
            suggestions.add(p);
        });

        studies.forEach(s -> {
            s.put("type", "study");
            s.put("icon", "Document");
            suggestions.add(s);
        });

        reports.forEach(r -> {
            r.put("type", "report");
            r.put("icon", "Files");
            suggestions.add(r);
        });

        treatments.forEach(t -> {
            t.put("type", "treatment");
            t.put("icon", "Checked");
            suggestions.add(t);
        });

        // Limit total suggestions
        return suggestions.stream()
                .limit(limit)
                .toList();
    }

    /**
     * Advanced search with filters
     */
    @Transactional(readOnly = true)
    public Map<String, Object> advancedSearch(
            String entityType,
            String query,
            Map<String, Object> filters,
            int page,
            int size) {

        Map<String, Object> results = new HashMap<>();

        switch (entityType.toLowerCase()) {
            case "patients":
                results = patientSearchService.advancedSearch(query, filters, page, size);
                break;
            case "studies":
                results = studySearchService.advancedSearch(query, filters, page, size);
                break;
            case "reports":
                results = reportSearchService.advancedSearch(query, filters, page, size);
                break;
            case "treatments":
                results = treatmentSearchService.advancedSearch(query, filters, page, size);
                break;
            default:
                results.put("results", new ArrayList<>());
                results.put("total", 0);
        }

        results.put("entityType", entityType);
        results.put("page", page);
        results.put("size", size);

        return results;
    }
}