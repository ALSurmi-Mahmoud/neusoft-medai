package com.team.medaibackend.service;

import com.team.medaibackend.entity.AuditLog;
import com.team.medaibackend.repository.AuditLogRepository;
import com.team.medaibackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User activity and engagement analytics
 */
@Service
public class UserAnalyticsService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public UserAnalyticsService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Get user activity metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActivityMetrics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(start, end);

        // Total actions
        metrics.put("totalActions", logs.size());

        // Unique active users
        long uniqueUsers = logs.stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        metrics.put("activeUsers", uniqueUsers);

        // Total registered users
        long totalUsers = userRepository.count();
        metrics.put("totalUsers", totalUsers);
        metrics.put("engagementRate", totalUsers > 0 ? (double) uniqueUsers / totalUsers * 100 : 0);

        // Actions per user
        metrics.put("averageActionsPerUser", uniqueUsers > 0 ?
                (double) logs.size() / uniqueUsers : 0);

        return metrics;
    }

    /**
     * Get most active users
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMostActiveUsers(LocalDate startDate, LocalDate endDate, int limit) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(start, end);

        Map<String, Long> userActionCounts = logs.stream()
                .filter(log -> log.getUsername() != null)
                .collect(Collectors.groupingBy(
                        AuditLog::getUsername,
                        Collectors.counting()
                ));

        return userActionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("username", entry.getKey());
                    item.put("actionCount", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get action distribution
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getActionDistribution(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(start, end);

        return logs.stream()
                .filter(log -> log.getAction() != null)
                .collect(Collectors.groupingBy(
                        AuditLog::getAction,
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
    }

    /**
     * Get resource type usage
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getResourceTypeUsage(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(start, end);

        return logs.stream()
                .filter(log -> log.getResourceType() != null)
                .collect(Collectors.groupingBy(
                        AuditLog::getResourceType,
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
     * Get activity trends over time
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivityTrends(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> trends = new ArrayList<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(start, end);

        // Group by date
        Map<LocalDate, Long> actionsByDate = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Fill in missing dates
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", current.toString());
            dataPoint.put("count", actionsByDate.getOrDefault(current, 0L));
            trends.add(dataPoint);
            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * Get hourly activity heatmap
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivityHeatmap() {
        List<AuditLog> logs = auditLogRepository.findAll();

        // Create 7x24 grid
        int[][] grid = new int[7][24];

        logs.stream()
                .filter(log -> log.getCreatedAt() != null)
                .forEach(log -> {
                    int dayOfWeek = log.getCreatedAt().getDayOfWeek().getValue() - 1; // 0-6
                    int hour = log.getCreatedAt().getHour(); // 0-23
                    grid[dayOfWeek][hour]++;
                });

        // Convert to list
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