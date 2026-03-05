package com.team.medaibackend.service;

import com.team.medaibackend.entity.TreatmentPlan;
import com.team.medaibackend.repository.TreatmentPlanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Treatment plan-specific search service
 */
@Service
public class TreatmentSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final TreatmentPlanRepository treatmentPlanRepository;

    public TreatmentSearchService(TreatmentPlanRepository treatmentPlanRepository) {
        this.treatmentPlanRepository = treatmentPlanRepository;
    }

    /**
     * Search treatment plans by query
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchTreatmentPlans(String query, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TreatmentPlan> cq = cb.createQuery(TreatmentPlan.class);
        Root<TreatmentPlan> plan = cq.from(TreatmentPlan.class);

        // Build search predicate
        String searchPattern = "%" + query.toLowerCase() + "%";

        Predicate searchPredicate = cb.or(
                cb.like(cb.lower(plan.get("planUid")), searchPattern),
                cb.like(cb.lower(plan.get("title")), searchPattern),
                cb.like(cb.lower(plan.get("diagnosis")), searchPattern),
                cb.like(cb.lower(plan.get("description")), searchPattern),
                cb.like(cb.lower(plan.get("goals")), searchPattern)
        );

        cq.where(searchPredicate);
        cq.orderBy(cb.desc(plan.get("createdAt")));

        List<TreatmentPlan> plans = entityManager.createQuery(cq)
                .setMaxResults(limit)
                .getResultList();

        return plans.stream()
                .map(this::mapTreatmentPlanToSearchResult)
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
        CriteriaQuery<TreatmentPlan> cq = cb.createQuery(TreatmentPlan.class);
        Root<TreatmentPlan> plan = cq.from(TreatmentPlan.class);

        List<Predicate> predicates = new ArrayList<>();

        // Text search
        if (query != null && !query.trim().isEmpty()) {
            String searchPattern = "%" + query.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(plan.get("planUid")), searchPattern),
                    cb.like(cb.lower(plan.get("title")), searchPattern),
                    cb.like(cb.lower(plan.get("diagnosis")), searchPattern),
                    cb.like(cb.lower(plan.get("description")), searchPattern)
            ));
        }

        // Apply filters
        if (filters != null) {
            // Status filter
            if (filters.containsKey("status")) {
                predicates.add(cb.equal(plan.get("status"), filters.get("status")));
            }

            // Priority filter
            if (filters.containsKey("priority")) {
                predicates.add(cb.equal(plan.get("priority"), filters.get("priority")));
            }

            // Category filter
            if (filters.containsKey("category")) {
                predicates.add(cb.equal(plan.get("category"), filters.get("category")));
            }

            // Doctor filter
            if (filters.containsKey("doctorId")) {
                predicates.add(cb.equal(plan.get("doctor").get("id"), filters.get("doctorId")));
            }

            // Patient filter
            if (filters.containsKey("patientId")) {
                predicates.add(cb.equal(plan.get("patient").get("id"), filters.get("patientId")));
            }

            // Progress range filter
            if (filters.containsKey("minProgress")) {
                int minProgress = ((Number) filters.get("minProgress")).intValue();
                predicates.add(cb.greaterThanOrEqualTo(plan.get("progressPercentage"), minProgress));
            }

            if (filters.containsKey("maxProgress")) {
                int maxProgress = ((Number) filters.get("maxProgress")).intValue();
                predicates.add(cb.lessThanOrEqualTo(plan.get("progressPercentage"), maxProgress));
            }

            // Start date range filter
            if (filters.containsKey("startDateFrom")) {
                LocalDate startDateFrom = (LocalDate) filters.get("startDateFrom");
                predicates.add(cb.greaterThanOrEqualTo(plan.get("startDate"), startDateFrom));
            }

            if (filters.containsKey("startDateTo")) {
                LocalDate startDateTo = (LocalDate) filters.get("startDateTo");
                predicates.add(cb.lessThanOrEqualTo(plan.get("startDate"), startDateTo));
            }

            // End date range filter
            if (filters.containsKey("endDateFrom")) {
                LocalDate endDateFrom = (LocalDate) filters.get("endDateFrom");
                predicates.add(cb.greaterThanOrEqualTo(plan.get("endDate"), endDateFrom));
            }

            if (filters.containsKey("endDateTo")) {
                LocalDate endDateTo = (LocalDate) filters.get("endDateTo");
                predicates.add(cb.lessThanOrEqualTo(plan.get("endDate"), endDateTo));
            }
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(cb.desc(plan.get("createdAt")));

        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<TreatmentPlan> countRoot = countQuery.from(TreatmentPlan.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Get paginated results
        List<TreatmentPlan> plans = entityManager.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        Map<String, Object> result = new HashMap<>();
        result.put("results", plans.stream().map(this::mapTreatmentPlanToSearchResult).toList());
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        return result;
    }

    /**
     * Quick filter: Active plans
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivePlans(int limit) {
        List<TreatmentPlan> plans = treatmentPlanRepository.findAll().stream()
                .filter(p -> "active".equals(p.getStatus()))
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(limit)
                .toList();

        return plans.stream()
                .map(this::mapTreatmentPlanToSearchResult)
                .toList();
    }

    /**
     * Quick filter: Overdue plans
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOverduePlans(int limit) {
        LocalDate today = LocalDate.now();

        List<TreatmentPlan> plans = treatmentPlanRepository.findAll().stream()
                .filter(p -> "active".equals(p.getStatus())
                        && p.getExpectedCompletionDate() != null
                        && p.getExpectedCompletionDate().isBefore(today))
                .sorted((p1, p2) -> p1.getExpectedCompletionDate().compareTo(p2.getExpectedCompletionDate()))
                .limit(limit)
                .toList();

        return plans.stream()
                .map(this::mapTreatmentPlanToSearchResult)
                .toList();
    }

    /**
     * Quick filter: High priority plans
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHighPriorityPlans(int limit) {
        List<TreatmentPlan> plans = treatmentPlanRepository.findAll().stream()
                .filter(p -> ("high".equals(p.getPriority()) || "urgent".equals(p.getPriority()))
                        && "active".equals(p.getStatus()))
                .sorted((p1, p2) -> {
                    // Sort by priority first (urgent > high), then by creation date
                    int priorityCompare = comparePriority(p2.getPriority(), p1.getPriority());
                    if (priorityCompare != 0) return priorityCompare;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .limit(limit)
                .toList();

        return plans.stream()
                .map(this::mapTreatmentPlanToSearchResult)
                .toList();
    }

    private int comparePriority(String priority1, String priority2) {
        Map<String, Integer> priorityValues = Map.of(
                "urgent", 4,
                "high", 3,
                "medium", 2,
                "low", 1
        );
        return priorityValues.getOrDefault(priority1, 0) - priorityValues.getOrDefault(priority2, 0);
    }

    private Map<String, Object> mapTreatmentPlanToSearchResult(TreatmentPlan plan) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", plan.getId());
        result.put("planUid", plan.getPlanUid());
        result.put("title", plan.getTitle());
        result.put("diagnosis", plan.getDiagnosis());
        result.put("status", plan.getStatus());
        result.put("priority", plan.getPriority());
        result.put("category", plan.getCategory());
        result.put("progressPercentage", plan.getProgressPercentage());
        result.put("startDate", plan.getStartDate());
        result.put("endDate", plan.getEndDate());
        result.put("expectedCompletionDate", plan.getExpectedCompletionDate());
        result.put("actualCompletionDate", plan.getActualCompletionDate());
        result.put("createdAt", plan.getCreatedAt());
        result.put("updatedAt", plan.getUpdatedAt());

        if (plan.getDoctor() != null) {
            result.put("doctorId", plan.getDoctor().getId());
            result.put("doctorName", plan.getDoctor().getUsername());
        }

        if (plan.getPatient() != null) {
            result.put("patientId", plan.getPatient().getId());
            result.put("patientName", plan.getPatient().getName());
        }

        return result;
    }
}