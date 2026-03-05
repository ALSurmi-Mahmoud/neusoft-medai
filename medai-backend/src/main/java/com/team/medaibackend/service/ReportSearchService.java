package com.team.medaibackend.service;

import com.team.medaibackend.entity.Report;
import com.team.medaibackend.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Report-specific search service
 */
@Service
public class ReportSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ReportRepository reportRepository;

    public ReportSearchService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Search reports by query
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchReports(String query, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Report> cq = cb.createQuery(Report.class);
        Root<Report> report = cq.from(Report.class);

        // Build search predicate
        String searchPattern = "%" + query.toLowerCase() + "%";

        Predicate searchPredicate = cb.or(
                cb.like(cb.lower(report.get("reportUid")), searchPattern),
                cb.like(cb.lower(report.get("title")), searchPattern),
                cb.like(cb.lower(report.get("summary")), searchPattern),
                cb.like(cb.lower(report.get("findings")), searchPattern),
                cb.like(cb.lower(report.get("impression")), searchPattern)
        );

        cq.where(searchPredicate);
        cq.orderBy(cb.desc(report.get("createdAt")));

        List<Report> reports = entityManager.createQuery(cq)
                .setMaxResults(limit)
                .getResultList();

        return reports.stream()
                .map(this::mapReportToSearchResult)
                .toList();
    }

    /**
     * Advanced search with filters
     */
    @Transactional(readOnly = true)
    public Map<String, Object> advancedSearch(
            String query,
            Map<String, Object> filters,
            int page,
            int size) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Report> cq = cb.createQuery(Report.class);
        Root<Report> report = cq.from(Report.class);

        List<Predicate> predicates = new ArrayList<>();

        // Text search
        if (query != null && !query.trim().isEmpty()) {
            String searchPattern = "%" + query.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(report.get("reportUid")), searchPattern),
                    cb.like(cb.lower(report.get("title")), searchPattern),
                    cb.like(cb.lower(report.get("summary")), searchPattern),
                    cb.like(cb.lower(report.get("findings")), searchPattern)
            ));
        }

        // Apply filters
        if (filters != null) {
            // Status filter
            if (filters.containsKey("status")) {
                predicates.add(cb.equal(report.get("status"), filters.get("status")));
            }

            // Finalized filter
            if (filters.containsKey("finalized")) {
                predicates.add(cb.equal(report.get("finalized"), filters.get("finalized")));
            }

            // Author filter
            if (filters.containsKey("authorId")) {
                predicates.add(cb.equal(report.get("author").get("id"), filters.get("authorId")));
            }

            // Patient filter
            if (filters.containsKey("patientId")) {
                predicates.add(cb.equal(report.get("patient").get("id"), filters.get("patientId")));
            }

            // Date range filter
            if (filters.containsKey("startDate")) {
                LocalDateTime startDate = (LocalDateTime) filters.get("startDate");
                predicates.add(cb.greaterThanOrEqualTo(report.get("createdAt"), startDate));
            }

            if (filters.containsKey("endDate")) {
                LocalDateTime endDate = (LocalDateTime) filters.get("endDate");
                predicates.add(cb.lessThanOrEqualTo(report.get("createdAt"), endDate));
            }

            // Finalized date range
            if (filters.containsKey("finalizedStartDate")) {
                LocalDateTime finalizedStartDate = (LocalDateTime) filters.get("finalizedStartDate");
                predicates.add(cb.greaterThanOrEqualTo(report.get("finalizedAt"), finalizedStartDate));
            }

            if (filters.containsKey("finalizedEndDate")) {
                LocalDateTime finalizedEndDate = (LocalDateTime) filters.get("finalizedEndDate");
                predicates.add(cb.lessThanOrEqualTo(report.get("finalizedAt"), finalizedEndDate));
            }
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(cb.desc(report.get("createdAt")));

        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Report> countRoot = countQuery.from(Report.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Get paginated results
        List<Report> reports = entityManager.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        Map<String, Object> result = new HashMap<>();
        result.put("results", reports.stream().map(this::mapReportToSearchResult).toList());
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        return result;
    }

    /**
     * Quick filter: Recent reports
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentReports(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<Report> reports = reportRepository.findAll().stream()
                .filter(r -> r.getCreatedAt().isAfter(since))
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .limit(limit)
                .toList();

        return reports.stream()
                .map(this::mapReportToSearchResult)
                .toList();
    }

    /**
     * Quick filter: Pending reports (drafts)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingReports(int limit) {
        List<Report> reports = reportRepository.findAll().stream()
                .filter(r -> "draft".equals(r.getStatus()) || !r.getFinalized())
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .limit(limit)
                .toList();

        return reports.stream()
                .map(this::mapReportToSearchResult)
                .toList();
    }

    private Map<String, Object> mapReportToSearchResult(Report report) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", report.getId());
        result.put("reportUid", report.getReportUid());
        result.put("title", report.getTitle());
        result.put("summary", report.getSummary());
        result.put("status", report.getStatus());
        result.put("finalized", report.getFinalized());
        result.put("createdAt", report.getCreatedAt());
        result.put("updatedAt", report.getUpdatedAt());
        result.put("finalizedAt", report.getFinalizedAt());

        if (report.getAuthor() != null) {
            result.put("authorId", report.getAuthor().getId());
            result.put("authorName", report.getAuthor().getUsername());
        }

        if (report.getPatient() != null) {
            result.put("patientId", report.getPatient().getId());
            result.put("patientName", report.getPatient().getName());
        }

        return result;
    }
}