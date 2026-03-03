package com.team.medaibackend.service;

import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Patient-specific analytics
 */
@Service
public class PatientAnalyticsService {

    private final PatientRepository patientRepository;

    public PatientAnalyticsService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Get patient demographics breakdown
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDemographics() {
        Map<String, Object> demographics = new HashMap<>();

        List<Patient> allPatients = patientRepository.findAll();

        // Age distribution
        Map<String, Long> ageGroups = allPatients.stream()
                .filter(p -> p.getBirthDate() != null)
                .collect(Collectors.groupingBy(
                        this::getAgeGroup,
                        Collectors.counting()
                ));
        demographics.put("ageDistribution", ageGroups);

        // Gender distribution
        Map<String, Long> genderDist = allPatients.stream()
                .filter(p -> p.getSex() != null)
                .collect(Collectors.groupingBy(
                        Patient::getSex,
                        Collectors.counting()
                ));
        demographics.put("genderDistribution", genderDist);

        // Blood type distribution
        Map<String, Long> bloodTypeDist = allPatients.stream()
                .filter(p -> p.getBloodType() != null)
                .collect(Collectors.groupingBy(
                        Patient::getBloodType,
                        Collectors.counting()
                ));
        demographics.put("bloodTypeDistribution", bloodTypeDist);

        // Geographic distribution (by city)
        Map<String, Long> geoDist = allPatients.stream()
                .filter(p -> p.getCity() != null)
                .collect(Collectors.groupingBy(
                        Patient::getCity,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        demographics.put("topCities", geoDist);

        // Insurance providers
        Map<String, Long> insuranceDist = allPatients.stream()
                .filter(p -> p.getInsuranceProvider() != null && !p.getInsuranceProvider().isEmpty())
                .collect(Collectors.groupingBy(
                        Patient::getInsuranceProvider,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        demographics.put("topInsuranceProviders", insuranceDist);

        return demographics;
    }

    /**
     * Get patient trends over time
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPatientTrends(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> trends = new ArrayList<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Patient> patients = patientRepository.findByCreatedAtBetween(start, end);

        // Group by date
        Map<LocalDate, Long> patientsByDate = patients.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Fill in missing dates with 0
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", current.toString());
            dataPoint.put("count", patientsByDate.getOrDefault(current, 0L));
            trends.add(dataPoint);
            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * Get common medical conditions
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCommonConditions() {
        List<Patient> patients = patientRepository.findAll();

        Map<String, Long> conditions = new HashMap<>();

        patients.stream()
                .filter(p -> p.getMedicalConditions() != null && !p.getMedicalConditions().isEmpty())
                .forEach(p -> {
                    // Split conditions by comma or semicolon
                    String[] patientConditions = p.getMedicalConditions().split("[,;]");
                    for (String condition : patientConditions) {
                        String trimmed = condition.trim();
                        if (!trimmed.isEmpty()) {
                            conditions.merge(trimmed, 1L, Long::sum);
                        }
                    }
                });

        // Return top 10
        return conditions.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Get common allergies
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCommonAllergies() {
        List<Patient> patients = patientRepository.findAll();

        Map<String, Long> allergies = new HashMap<>();

        patients.stream()
                .filter(p -> p.getAllergies() != null && !p.getAllergies().isEmpty())
                .forEach(p -> {
                    String[] patientAllergies = p.getAllergies().split("[,;]");
                    for (String allergy : patientAllergies) {
                        String trimmed = allergy.trim();
                        if (!trimmed.isEmpty()) {
                            allergies.merge(trimmed, 1L, Long::sum);
                        }
                    }
                });

        return allergies.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Get patient statistics summary
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPatientStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long total = patientRepository.count();
        stats.put("totalPatients", total);

        // Patients with complete profiles
        List<Patient> allPatients = patientRepository.findAll();
        long completeProfiles = allPatients.stream()
                .filter(this::isProfileComplete)
                .count();
        stats.put("completeProfiles", completeProfiles);
        stats.put("profileCompletionRate", total > 0 ? (double) completeProfiles / total * 100 : 0);

        // Average age
        double avgAge = allPatients.stream()
                .filter(p -> p.getBirthDate() != null)
                .mapToInt(this::calculateAge)
                .average()
                .orElse(0);
        stats.put("averageAge", Math.round(avgAge * 10) / 10.0);

        return stats;
    }

    // Helper methods
    private String getAgeGroup(Patient patient) {
        int age = calculateAge(patient);
        if (age < 18) return "0-17";
        if (age < 30) return "18-29";
        if (age < 45) return "30-44";
        if (age < 60) return "45-59";
        if (age < 75) return "60-74";
        return "75+";
    }

    private int calculateAge(Patient patient) {
        if (patient.getBirthDate() == null) return 0;
        return Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
    }

    private boolean isProfileComplete(Patient patient) {
        return patient.getName() != null &&
                patient.getBirthDate() != null &&
                patient.getSex() != null &&
                patient.getPhone() != null &&
                patient.getEmail() != null;
    }
}