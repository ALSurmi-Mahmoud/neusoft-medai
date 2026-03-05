package com.team.medaibackend.service;

import com.team.medaibackend.entity.Study;
import com.team.medaibackend.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Study-specific search service
 */
@Service
public class StudySearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final StudyRepository studyRepository;

    public StudySearchService(StudyRepository studyRepository) {
        this.studyRepository = studyRepository;
    }

    /**
     * Search studies by query
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchStudies(String query, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Study> cq = cb.createQuery(Study.class);
        Root<Study> study = cq.from(Study.class);

        String searchPattern = "%" + query.toLowerCase() + "%";

        Predicate searchPredicate = cb.or(
                cb.like(cb.lower(study.get("accessionNumber")), searchPattern),
                cb.like(cb.lower(study.get("description")), searchPattern),
                cb.like(cb.lower(study.get("modality")), searchPattern)
        );

        cq.where(searchPredicate);
        cq.orderBy(cb.desc(study.get("studyDate")));

        List<Study> studies = entityManager.createQuery(cq)
                .setMaxResults(limit)
                .getResultList();

        return studies.stream()
                .map(this::mapStudyToSearchResult)
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
        CriteriaQuery<Study> cq = cb.createQuery(Study.class);
        Root<Study> study = cq.from(Study.class);

        List<Predicate> predicates = new ArrayList<>();

        // Text search
        if (query != null && !query.trim().isEmpty()) {
            String searchPattern = "%" + query.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(study.get("accessionNumber")), searchPattern),
                    cb.like(cb.lower(study.get("description")), searchPattern)
            ));
        }

        // Apply filters
        if (filters != null) {
            // Modality filter
            if (filters.containsKey("modality")) {
                predicates.add(cb.equal(study.get("modality"), filters.get("modality")));
            }

            // Status filter
            if (filters.containsKey("status")) {
                predicates.add(cb.equal(study.get("status"), filters.get("status")));
            }

            // Date range filter
            if (filters.containsKey("startDate")) {
                LocalDateTime startDate = LocalDateTime.parse(filters.get("startDate").toString());
                predicates.add(cb.greaterThanOrEqualTo(study.get("studyDate"), startDate));
            }

            if (filters.containsKey("endDate")) {
                LocalDateTime endDate = LocalDateTime.parse(filters.get("endDate").toString());
                predicates.add(cb.lessThanOrEqualTo(study.get("studyDate"), endDate));
            }
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(cb.desc(study.get("studyDate")));

        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Study> countRoot = countQuery.from(Study.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Get paginated results
        List<Study> studies = entityManager.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        Map<String, Object> result = new HashMap<>();
        result.put("results", studies.stream().map(this::mapStudyToSearchResult).toList());
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        return result;
    }

    /**
     * Quick filter: Today's studies
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTodaysStudies(int limit) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Study> studies = studyRepository.findAll().stream()
                .filter(s -> s.getStudyDate() != null &&
                        s.getStudyDate().isAfter(startOfDay) &&
                        s.getStudyDate().isBefore(endOfDay))
                .sorted((s1, s2) -> s2.getStudyDate().compareTo(s1.getStudyDate()))
                .limit(limit)
                .toList();

        return studies.stream()
                .map(this::mapStudyToSearchResult)
                .toList();
    }

    private Map<String, Object> mapStudyToSearchResult(Study study) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", study.getId());
        result.put("accessionNumber", study.getAccessionNumber());
        result.put("description", study.getDescription());
        result.put("modality", study.getModality());
        result.put("studyDate", study.getStudyDate());
        result.put("status", study.getStatus());

        if (study.getPatient() != null) {
            result.put("patientName", study.getPatient().getName());
            result.put("patientId", study.getPatient().getPatientId());
        }

        return result;
    }
}