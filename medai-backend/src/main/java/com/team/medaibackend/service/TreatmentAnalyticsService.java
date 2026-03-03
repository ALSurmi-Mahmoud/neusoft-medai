package com.team.medaibackend.service;

import com.team.medaibackend.entity.TreatmentPlan;
import com.team.medaibackend.repository.TreatmentPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Treatment plan analytics
 */
@Service
public class TreatmentAnalyticsService {

    private final TreatmentPlanRepository treatmentPlanRepository;

    public TreatmentAnalyticsService(TreatmentPlanRepository treatmentPlanRepository) {
        this.treatmentPlanRepository = treatmentPlanRepository;
    }

    /**
     * Get treatment overview metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();

        List<TreatmentPlan> allPlans = treatmentPlanRepository.findAll();

        // Status distribution
        Map<String, Long> statusDist = allPlans.stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(
                        TreatmentPlan::getStatus,
                        Collectors.counting()
                ));
        overview.put("byStatus", statusDist);

        // Active vs completed
        long active = statusDist.getOrDefault("active", 0L);
        long completed = statusDist.getOrDefault("completed", 0L);
        long total = active + completed;

        overview.put("activePlans", active);
        overview.put("completedPlans", completed);
        overview.put("completionRate", total > 0 ? (double) completed / total * 100 : 0);

        // Average progress
        double avgProgress = allPlans.stream()
                .filter(p -> "active".equals(p.getStatus()) && p.getProgressPercentage() != null)
                .mapToInt(TreatmentPlan::getProgressPercentage)
                .average()
                .orElse(0);
        overview.put("averageProgress", Math.round(avgProgress * 10) / 10.0);

        return overview;
    }

    /**
     * Get plans by category
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getPlansByCategory() {
        List<TreatmentPlan> plans = treatmentPlanRepository.findAll();

        return plans.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(
                        TreatmentPlan::getCategory,
                        Collectors.counting()
                ));
    }

    /**
     * Get plans by priority
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getPlansByPriority() {
        List<TreatmentPlan> plans = treatmentPlanRepository.findAll();

        return plans.stream()
                .filter(p -> p.getPriority() != null)
                .collect(Collectors.groupingBy(
                        TreatmentPlan::getPriority,
                        Collectors.counting()
                ));
    }

    /**
     * Get treatment plan trends
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTreatmentTrends(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> trends = new ArrayList<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<TreatmentPlan> plans = treatmentPlanRepository.findByCreatedAtBetween(start, end);

        // Group by date
        Map<LocalDate, Long> plansByDate = plans.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Fill in missing dates
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", current.toString());
            dataPoint.put("count", plansByDate.getOrDefault(current, 0L));
            trends.add(dataPoint);
            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * Get success rate analysis
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSuccessRateAnalysis() {
        Map<String, Object> analysis = new HashMap<>();

        List<TreatmentPlan> plans = treatmentPlanRepository.findAll();

        long completed = plans.stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .count();

        long cancelled = plans.stream()
                .filter(p -> "cancelled".equals(p.getStatus()))
                .count();

        long total = plans.size();

        analysis.put("totalPlans", total);
        analysis.put("completed", completed);
        analysis.put("cancelled", cancelled);
        analysis.put("successRate", total > 0 ? (double) completed / total * 100 : 0);
        analysis.put("cancellationRate", total > 0 ? (double) cancelled / total * 100 : 0);

        return analysis;
    }

    /**
     * Get average plan duration
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDurationAnalysis() {
        Map<String, Object> analysis = new HashMap<>();

        List<TreatmentPlan> completedPlans = treatmentPlanRepository.findAll().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .filter(p -> p.getStartDate() != null && p.getActualCompletionDate() != null)
                .collect(Collectors.toList());

        if (completedPlans.isEmpty()) {
            analysis.put("averageDays", 0);
            analysis.put("minDays", 0);
            analysis.put("maxDays", 0);
            return analysis;
        }

        List<Long> durations = completedPlans.stream()
                .map(p -> ChronoUnit.DAYS.between(p.getStartDate(), p.getActualCompletionDate()))
                .sorted()
                .collect(Collectors.toList());

        double average = durations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        analysis.put("averageDays", Math.round(average * 10) / 10.0);
        analysis.put("minDays", durations.get(0));
        analysis.put("maxDays", durations.get(durations.size() - 1));

        return analysis;
    }

    /**
     * Get progress distribution
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getProgressDistribution() {
        List<TreatmentPlan> activePlans = treatmentPlanRepository.findAll().stream()
                .filter(p -> "active".equals(p.getStatus()))
                .filter(p -> p.getProgressPercentage() != null)
                .collect(Collectors.toList());

        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0-25%", activePlans.stream().filter(p -> p.getProgressPercentage() <= 25).count());
        distribution.put("26-50%", activePlans.stream().filter(p -> p.getProgressPercentage() > 25 && p.getProgressPercentage() <= 50).count());
        distribution.put("51-75%", activePlans.stream().filter(p -> p.getProgressPercentage() > 50 && p.getProgressPercentage() <= 75).count());
        distribution.put("76-100%", activePlans.stream().filter(p -> p.getProgressPercentage() > 75).count());

        return distribution;
    }
}