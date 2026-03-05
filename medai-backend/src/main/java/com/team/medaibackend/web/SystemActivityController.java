package com.team.medaibackend.web;

import com.team.medaibackend.entity.SystemActivityLog;
import com.team.medaibackend.service.SystemActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/activity-logs")
public class SystemActivityController {

    private final SystemActivityService activityService;

    public SystemActivityController(SystemActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SystemActivityLog> activities = activityService.getActivities(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", activities.getContent().stream()
                    .map(this::buildActivityDto)
                    .collect(Collectors.toList()));
            response.put("totalElements", activities.getTotalElements());
            response.put("totalPages", activities.getTotalPages());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRecentActivities(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<SystemActivityLog> activities = activityService.getRecentActivities(days);
            List<Map<String, Object>> result = activities.stream()
                    .map(this::buildActivityDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/by-type/{activityType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getActivitiesByType(
            @PathVariable String activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SystemActivityLog> activities =
                    activityService.getActivitiesByType(activityType, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", activities.getContent().stream()
                    .map(this::buildActivityDto)
                    .collect(Collectors.toList()));
            response.put("totalElements", activities.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> buildActivityDto(SystemActivityLog log) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", log.getId());
        dto.put("activityType", log.getActivityType());
        dto.put("description", log.getDescription());
        dto.put("status", log.getStatus());
        dto.put("ipAddress", log.getIpAddress());
        dto.put("metadata", log.getMetadata());
        dto.put("createdAt", log.getCreatedAt());

        if (log.getPerformedBy() != null) {
            dto.put("performedBy", log.getPerformedBy().getUsername());
            dto.put("performedByName", log.getPerformedBy().getFullName());
        }

        return dto;
    }
}