package com.team.medaibackend.service;

import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.repository.PatientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Patient-specific search service
 */
@Service
public class PatientSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final PatientRepository patientRepository;

    public PatientSearchService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Search patients by query
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchPatients(String query, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Patient> cq = cb.createQuery(Patient.class);
        Root<Patient> patient = cq.from(Patient.class);

        // Build search predicate
        String searchPattern = "%" + query.toLowerCase() + "%";

        Predicate searchPredicate = cb.or(
                cb.like(cb.lower(patient.get("name")), searchPattern),
                cb.like(cb.lower(patient.get("patientId")), searchPattern),
                cb.like(cb.lower(patient.get("email")), searchPattern),
                cb.like(cb.lower(patient.get("phone")), searchPattern)
        );

        cq.where(searchPredicate);
        cq.orderBy(cb.desc(patient.get("createdAt")));

        List<Patient> patients = entityManager.createQuery(cq)
                .setMaxResults(limit)
                .getResultList();

        return patients.stream()
                .map(this::mapPatientToSearchResult)
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
        CriteriaQuery<Patient> cq = cb.createQuery(Patient.class);
        Root<Patient> patient = cq.from(Patient.class);

        List<Predicate> predicates = new ArrayList<>();

        // Text search
        if (query != null && !query.trim().isEmpty()) {
            String searchPattern = "%" + query.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(patient.get("name")), searchPattern),
                    cb.like(cb.lower(patient.get("patientId")), searchPattern),
                    cb.like(cb.lower(patient.get("email")), searchPattern)
            ));
        }

        // Apply filters
        if (filters != null) {
            // Gender filter
            if (filters.containsKey("sex")) {
                predicates.add(cb.equal(patient.get("sex"), filters.get("sex")));
            }

            // Blood type filter
            if (filters.containsKey("bloodType")) {
                predicates.add(cb.equal(patient.get("bloodType"), filters.get("bloodType")));
            }

            // City filter
            if (filters.containsKey("city")) {
                predicates.add(cb.equal(patient.get("city"), filters.get("city")));
            }

            // Age range filter
            if (filters.containsKey("minAge") || filters.containsKey("maxAge")) {
                LocalDate today = LocalDate.now();

                if (filters.containsKey("minAge")) {
                    int minAge = ((Number) filters.get("minAge")).intValue();
                    LocalDate maxBirthDate = today.minusYears(minAge);
                    predicates.add(cb.lessThanOrEqualTo(patient.get("birthDate"), maxBirthDate));
                }

                if (filters.containsKey("maxAge")) {
                    int maxAge = ((Number) filters.get("maxAge")).intValue();
                    LocalDate minBirthDate = today.minusYears(maxAge + 1);
                    predicates.add(cb.greaterThan(patient.get("birthDate"), minBirthDate));
                }
            }

            // Date range filter
            if (filters.containsKey("startDate")) {
                LocalDateTime startDate = (LocalDateTime) filters.get("startDate");
                predicates.add(cb.greaterThanOrEqualTo(
                        patient.get("createdAt"),
                        startDate
                ));
            }

            if (filters.containsKey("endDate")) {
                LocalDateTime endDate = (LocalDateTime) filters.get("endDate");
                predicates.add(cb.lessThanOrEqualTo(
                        patient.get("createdAt"),
                        endDate
                ));
            }
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(cb.desc(patient.get("createdAt")));

        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Patient> countRoot = countQuery.from(Patient.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Get paginated results
        List<Patient> patients = entityManager.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        Map<String, Object> result = new HashMap<>();
        result.put("results", patients.stream().map(this::mapPatientToSearchResult).toList());
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        return result;
    }

    /**
     * Quick filter: New patients
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNewPatients(int days, int limit) {
        LocalDate since = LocalDate.now().minusDays(days);

        List<Patient> patients = patientRepository.findAll().stream()
                .filter(p -> p.getCreatedAt().toLocalDate().isAfter(since))
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(limit)
                .toList();

        return patients.stream()
                .map(this::mapPatientToSearchResult)
                .toList();
    }

    private Map<String, Object> mapPatientToSearchResult(Patient patient) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", patient.getId());
        result.put("patientId", patient.getPatientId());
        result.put("name", patient.getName());
        result.put("birthDate", patient.getBirthDate());
        result.put("sex", patient.getSex());
        result.put("phone", patient.getPhone());
        result.put("email", patient.getEmail());
        result.put("city", patient.getCity());
        result.put("bloodType", patient.getBloodType());
        result.put("createdAt", patient.getCreatedAt());
        return result;
    }
}