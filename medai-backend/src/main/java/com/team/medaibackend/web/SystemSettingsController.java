package com.team.medaibackend.web;

import com.team.medaibackend.entity.SystemSettings;
import com.team.medaibackend.service.SystemSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class SystemSettingsController {

    private final SystemSettingsService settingsService;

    public SystemSettingsController(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSettings() {
        try {
            SystemSettings settings = settingsService.getSettings();
            return ResponseEntity.ok(buildSettingsDto(settings));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> request) {
        try {
            SystemSettings updates = new SystemSettings();

            if (request.containsKey("appName")) {
                updates.setAppName((String) request.get("appName"));
            }
            if (request.containsKey("smtpEnabled")) {
                updates.setSmtpEnabled((Boolean) request.get("smtpEnabled"));
            }
            if (request.containsKey("smtpHost")) {
                updates.setSmtpHost((String) request.get("smtpHost"));
            }
            if (request.containsKey("sessionTimeoutMinutes")) {
                updates.setSessionTimeoutMinutes(
                        ((Number) request.get("sessionTimeoutMinutes")).intValue());
            }
            if (request.containsKey("maintenanceMode")) {
                updates.setMaintenanceMode((Boolean) request.get("maintenanceMode"));
            }

            SystemSettings saved = settingsService.updateSettings(updates);
            return ResponseEntity.ok(buildSettingsDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/maintenance-status")
    public ResponseEntity<?> getMaintenanceStatus() {
        boolean maintenanceMode = settingsService.isMaintenanceMode();
        return ResponseEntity.ok(Map.of("maintenanceMode", maintenanceMode));
    }

    private Map<String, Object> buildSettingsDto(SystemSettings settings) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", settings.getId());
        dto.put("appName", settings.getAppName());
        dto.put("appVersion", settings.getAppVersion());
        dto.put("smtpEnabled", settings.getSmtpEnabled());
        dto.put("smtpHost", settings.getSmtpHost());
        dto.put("smtpPort", settings.getSmtpPort());
        dto.put("sessionTimeoutMinutes", settings.getSessionTimeoutMinutes());
        dto.put("passwordMinLength", settings.getPasswordMinLength());
        dto.put("maxLoginAttempts", settings.getMaxLoginAttempts());
        dto.put("maxUploadSizeMb", settings.getMaxUploadSizeMb());
        dto.put("storageQuotaGb", settings.getStorageQuotaGb());
        dto.put("maintenanceMode", settings.getMaintenanceMode());
        dto.put("maintenanceMessage", settings.getMaintenanceMessage());
        return dto;
    }
}