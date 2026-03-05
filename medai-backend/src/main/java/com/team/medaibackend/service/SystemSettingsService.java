package com.team.medaibackend.service;

import com.team.medaibackend.entity.SystemSettings;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.SystemSettingsRepository;
import com.team.medaibackend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingsService {

    private final SystemSettingsRepository settingsRepository;
    private final SecurityUtils securityUtils;
    private final SystemActivityService activityService;

    public SystemSettingsService(
            SystemSettingsRepository settingsRepository,
            SecurityUtils securityUtils,
            SystemActivityService activityService) {
        this.settingsRepository = settingsRepository;
        this.securityUtils = securityUtils;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public SystemSettings getSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    SystemSettings settings = new SystemSettings();
                    return settingsRepository.save(settings);
                });
    }

    @Transactional
    public SystemSettings updateSettings(SystemSettings updates) {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        SystemSettings settings = getSettings();

        // Update fields
        if (updates.getAppName() != null) {
            settings.setAppName(updates.getAppName());
        }
        if (updates.getSmtpEnabled() != null) {
            settings.setSmtpEnabled(updates.getSmtpEnabled());
        }
        if (updates.getSmtpHost() != null) {
            settings.setSmtpHost(updates.getSmtpHost());
        }
        if (updates.getSmtpPort() != null) {
            settings.setSmtpPort(updates.getSmtpPort());
        }
        if (updates.getSessionTimeoutMinutes() != null) {
            settings.setSessionTimeoutMinutes(updates.getSessionTimeoutMinutes());
        }
        if (updates.getMaintenanceMode() != null) {
            settings.setMaintenanceMode(updates.getMaintenanceMode());
        }
        if (updates.getMaintenanceMessage() != null) {
            settings.setMaintenanceMessage(updates.getMaintenanceMessage());
        }

        settings.setUpdatedBy(currentUser);

        SystemSettings saved = settingsRepository.save(settings);

        // Log activity
        activityService.logActivity(
                "SETTINGS_UPDATE",
                "System settings updated",
                currentUser,
                "success"
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isMaintenanceMode() {
        SystemSettings settings = getSettings();
        return settings.getMaintenanceMode() != null && settings.getMaintenanceMode();
    }
}