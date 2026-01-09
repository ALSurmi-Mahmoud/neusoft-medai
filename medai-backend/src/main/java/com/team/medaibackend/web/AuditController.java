package com.team.medaibackend.web;

import com.team.medaibackend.entity.AuditLog;
import com.team.medaibackend.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Get audit logs with filters.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<AuditLog> logs = auditService.getAuditLogs(
                userId, action, resourceType, dateFrom, dateTo, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", logs.getContent());
        response.put("totalElements", logs.getTotalElements());
        response.put("totalPages", logs.getTotalPages());
        response.put("currentPage", logs.getNumber());
        response.put("pageSize", logs.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * Get available audit action types.
     */
    @GetMapping("/actions")
    public ResponseEntity<Map<String, Object>> getAuditActions() {
        Map<String, Object> response = new HashMap<>();
        response.put("actions", Map.of(
                "LOGIN", "User login",
                "LOGOUT", "User logout",
                "UPLOAD", "File upload",
                "CREATE", "Resource creation",
                "UPDATE", "Resource update",
                "DELETE", "Resource deletion",
                "VIEW", "Resource view",
                "EXPORT", "Data export",
                "FINALIZE", "Report finalization"
        ));
        return ResponseEntity.ok(response);
    }
}