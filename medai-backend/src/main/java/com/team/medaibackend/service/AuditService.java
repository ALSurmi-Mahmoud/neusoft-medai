package com.team.medaibackend.service;

import com.team.medaibackend.entity.AuditLog;
import com.team.medaibackend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String action, String resourceType, String resourceId,
                    Long userId, String username,
                    String ipAddress, String userAgent,
                    Map<String, Object> details) {

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);

        logger.info("AUDIT: {} - {} {} by user {} ({})",
                action, resourceType, resourceId, username, ipAddress);
    }

    @Transactional
    public void log(String action, String resourceType, String resourceId,
                    Long userId, String username) {
        log(action, resourceType, resourceId, userId, username, null, null, null);
    }

    @Transactional
    public void logAnonymous(String action, String resourceType, String resourceId,
                             String ipAddress, Map<String, Object> details) {
        log(action, resourceType, resourceId, null, "anonymous", ipAddress, null, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Long userId, String action, String resourceType,
                                       String dateFrom, String dateTo,
                                       int page, int size) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDateTime dateFromParsed = dateFrom != null ?
                LocalDateTime.parse(dateFrom + "T00:00:00") : null;
        LocalDateTime dateToParsed = dateTo != null ?
                LocalDateTime.parse(dateTo + "T23:59:59") : null;

        return auditLogRepository.findByFilters(
                userId, action, resourceType, dateFromParsed, dateToParsed, pageable);
    }

    @Transactional
    public void logAnonymous(String action, String resourceType, String resourceId) {
        log(action, resourceType, resourceId, null, "anonymous", null, null, null);
    }

    // Common audit actions
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_UPLOAD = "UPLOAD";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_FINALIZE = "FINALIZE";
}