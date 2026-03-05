package com.team.medaibackend.service;

import com.team.medaibackend.entity.SystemActivityLog;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.SystemActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SystemActivityService {

    private final SystemActivityLogRepository activityLogRepository;

    public SystemActivityService(SystemActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void logActivity(String activityType, String description, User user, String status) {
        logActivity(activityType, description, user, status, null, null);
    }

    @Transactional
    public void logActivity(
            String activityType,
            String description,
            User user,
            String status,
            String ipAddress,
            Map<String, Object> metadata) {

        SystemActivityLog log = new SystemActivityLog();
        log.setActivityType(activityType);
        log.setDescription(description);
        log.setPerformedBy(user);
        log.setStatus(status);
        log.setIpAddress(ipAddress);
        log.setMetadata(metadata);

        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<SystemActivityLog> getActivities(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<SystemActivityLog> getRecentActivities(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return activityLogRepository.findRecentActivities(since);
    }

    @Transactional(readOnly = true)
    public Page<SystemActivityLog> getActivitiesByType(String activityType, Pageable pageable) {
        return activityLogRepository.findByActivityTypeOrderByCreatedAtDesc(activityType, pageable);
    }
}