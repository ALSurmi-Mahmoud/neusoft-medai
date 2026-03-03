package com.team.medaibackend.service;

import com.team.medaibackend.entity.Study;
import com.team.medaibackend.repository.StudyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Study and imaging analytics
 */
@Service
public class StudyAnalyticsService {

    private final StudyRepository studyRepository;

    public StudyAnalyticsService(StudyRepository studyRepository) {
        this.studyRepository = studyRepository;
    }

    /**
     * Get studies by modality
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStudiesByModality() {
        List<Study> studies = studyRepository.findAll();

        return studies.stream()
                .filter(s -> s.getModality() != null)
                .collect(Collectors.groupingBy(
                        Study::getModality,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Get study volume trends
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudyTrends(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> trends = new ArrayList<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Study> studies = studyRepository.findByCreatedAtBetween(start, end);

        // Group by date
        Map<LocalDate, Long> studiesByDate = studies.stream()
                .filter(s -> s.getStudyDate() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getStudyDate().toLocalDate(),
                        Collectors.counting()
                ));

        // Fill in missing dates
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", current.toString());
            dataPoint.put("count", studiesByDate.getOrDefault(current, 0L));
            trends.add(dataPoint);
            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * Get study status distribution
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStudyStatusDistribution() {
        List<Study> studies = studyRepository.findAll();

        return studies.stream()
                .filter(s -> s.getStatus() != null)
                .collect(Collectors.groupingBy(
                        Study::getStatus,
                        Collectors.counting()
                ));
    }

    /**
     * Get studies by modality over time
     */
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> getModalityTrends(
            LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Study> studies = studyRepository.findByCreatedAtBetween(start, end);

        // Group by modality, then by date
        Map<String, Map<LocalDate, Long>> modalityDateCounts = studies.stream()
                .filter(s -> s.getModality() != null && s.getStudyDate() != null)
                .collect(Collectors.groupingBy(
                        Study::getModality,
                        Collectors.groupingBy(
                                s -> s.getStudyDate().toLocalDate(),
                                Collectors.counting()
                        )
                ));

        // Convert to chart-friendly format
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        modalityDateCounts.forEach((modality, dateCounts) -> {
            List<Map<String, Object>> series = new ArrayList<>();

            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", current.toString());
                dataPoint.put("count", dateCounts.getOrDefault(current, 0L));
                series.add(dataPoint);
                current = current.plusDays(1);
            }

            result.put(modality, series);
        });

        return result;
    }

    /**
     * Get study performance metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        List<Study> studies = studyRepository.findAll();

        // Total studies
        metrics.put("totalStudies", studies.size());

        // Studies by status
        Map<String, Long> statusCounts = studies.stream()
                .filter(s -> s.getStatus() != null)
                .collect(Collectors.groupingBy(
                        Study::getStatus,
                        Collectors.counting()
                ));
        metrics.put("byStatus", statusCounts);

        // Average studies per day (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentStudies = studies.stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();
        metrics.put("averagePerDay", Math.round(recentStudies / 30.0 * 10) / 10.0);

        // Most active modality
        String mostActiveModality = studies.stream()
                .filter(s -> s.getModality() != null)
                .collect(Collectors.groupingBy(
                        Study::getModality,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        metrics.put("mostActiveModality", mostActiveModality);

        return metrics;
    }

    /**
     * Get hourly activity pattern (heatmap data)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHourlyActivity() {
        List<Study> studies = studyRepository.findAll();

        // Create 7x24 grid (day of week x hour)
        int[][] grid = new int[7][24];

        studies.stream()
                .filter(s -> s.getStudyDate() != null)
                .forEach(s -> {
                    int dayOfWeek = s.getStudyDate().getDayOfWeek().getValue() - 1; // 0-6
                    int hour = s.getStudyDate().getHour(); // 0-23
                    grid[dayOfWeek][hour]++;
                });

        // Convert to list format
        List<Map<String, Object>> heatmapData = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 24; hour++) {
                Map<String, Object> cell = new HashMap<>();
                cell.put("day", days[day]);
                cell.put("hour", hour);
                cell.put("value", grid[day][hour]);
                heatmapData.add(cell);
            }
        }

        return heatmapData;
    }
}