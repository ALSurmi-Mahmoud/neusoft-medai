package com.team.medaibackend.service;

import com.team.medaibackend.entity.Report;
import com.team.medaibackend.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Report production and quality analytics
 */
@Service
public class ReportAnalyticsService {

    private final ReportRepository reportRepository;

    public ReportAnalyticsService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Get report production metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductionMetrics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Report> reports = reportRepository.findByCreatedAtBetween(start, end);

        // Total reports created
        metrics.put("totalReports", reports.size());

        // Reports by status
        Map<String, Long> statusDist = reports.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(
                        Report::getStatus,
                        Collectors.counting()
                ));
        metrics.put("byStatus", statusDist);

        // Finalized vs draft ratio
        long finalized = statusDist.getOrDefault("finalized", 0L);
        long total = reports.size();
        metrics.put("finalizedPercentage", total > 0 ? (double) finalized / total * 100 : 0);

        // Average reports per day
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        metrics.put("averagePerDay", daysBetween > 0 ? (double) total / daysBetween : 0);

        return metrics;
    }

    /**
     * Get report trends over time
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReportTrends(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> trends = new ArrayList<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Report> reports = reportRepository.findByCreatedAtBetween(start, end);

        // Group by date
        Map<LocalDate, Long> reportsByDate = reports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Fill in missing dates
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", current.toString());
            dataPoint.put("count", reportsByDate.getOrDefault(current, 0L));
            trends.add(dataPoint);
            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * Get reports by doctor (productivity)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReportsByDoctor(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Report> reports = reportRepository.findByCreatedAtBetween(start, end);

        Map<String, Long> doctorCounts = reports.stream()
                .filter(r -> r.getAuthor() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getAuthor().getFullName() != null ?
                                r.getAuthor().getFullName() : "Unknown",
                        Collectors.counting()
                ));

        // Convert to list and sort
        return doctorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("doctor", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get report quality metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQualityMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        List<Report> allReports = reportRepository.findAll();

        // Average time to finalize (for finalized reports)
        List<Report> finalizedReports = allReports.stream()
                .filter(r -> "finalized".equals(r.getStatus()) && r.getFinalizedAt() != null)
                .collect(Collectors.toList());

        if (!finalizedReports.isEmpty()) {
            double avgHours = finalizedReports.stream()
                    .mapToLong(r -> ChronoUnit.HOURS.between(
                            r.getCreatedAt(), r.getFinalizedAt()))
                    .average()
                    .orElse(0);
            metrics.put("averageTimeToFinalize", Math.round(avgHours * 10) / 10.0);
        } else {
            metrics.put("averageTimeToFinalize", 0);
        }

        // Amendment rate
        long amended = allReports.stream()
                .filter(r -> "amended".equals(r.getStatus()))
                .count();
        metrics.put("amendmentRate", allReports.size() > 0 ?
                (double) amended / allReports.size() * 100 : 0);

        // Reports with study link
        long withStudy = allReports.stream()
                .filter(r -> r.getStudy() != null)
                .count();
        metrics.put("studyLinkRate", allReports.size() > 0 ?
                (double) withStudy / allReports.size() * 100 : 0);

        return metrics;
    }

    /**
     * Get report status distribution
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatusDistribution() {
        List<Report> reports = reportRepository.findAll();

        return reports.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(
                        Report::getStatus,
                        Collectors.counting()
                ));
    }

    /**
     * Get turnaround time analysis
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTurnaroundAnalysis() {
        Map<String, Object> analysis = new HashMap<>();

        List<Report> reports = reportRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && r.getFinalizedAt() != null)
                .collect(Collectors.toList());

        if (reports.isEmpty()) {
            analysis.put("average", 0);
            analysis.put("min", 0);
            analysis.put("max", 0);
            return analysis;
        }

        List<Long> turnaroundHours = reports.stream()
                .map(r -> ChronoUnit.HOURS.between(r.getCreatedAt(), r.getFinalizedAt()))
                .sorted()
                .collect(Collectors.toList());

        double average = turnaroundHours.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        analysis.put("average", Math.round(average * 10) / 10.0);
        analysis.put("min", turnaroundHours.get(0));
        analysis.put("max", turnaroundHours.get(turnaroundHours.size() - 1));
        analysis.put("median", turnaroundHours.get(turnaroundHours.size() / 2));

        return analysis;
    }
}