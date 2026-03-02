package com.team.medaibackend.service;

import com.team.medaibackend.entity.ReportTemplate;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.ReportTemplateRepository;
import com.team.medaibackend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportTemplateService {

    private final ReportTemplateRepository templateRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public ReportTemplateService(
            ReportTemplateRepository templateRepository,
            SecurityUtils securityUtils,
            AuditService auditService) {
        this.templateRepository = templateRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ReportTemplate> getAccessibleTemplates() {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        return templateRepository.findAccessibleTemplates(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public List<ReportTemplate> getTemplatesByCategory(String category) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        return templateRepository.findByCategoryForUser(category, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public ReportTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
    }

    @Transactional
    public ReportTemplate createTemplate(ReportTemplate template) {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        if (templateRepository.existsByNameAndCreatedById(template.getName(), currentUser.getId())) {
            throw new RuntimeException("Template with this name already exists");
        }

        template.setCreatedBy(currentUser);
        template.setIsSystem(false);
        template.setIsActive(true);
        template.setUsageCount(0);

        ReportTemplate saved = templateRepository.save(template);

        auditService.log(
                AuditService.ACTION_CREATE,
                "REPORT_TEMPLATE",
                saved.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    @Transactional
    public ReportTemplate updateTemplate(Long id, ReportTemplate updates) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        ReportTemplate existing = getTemplateById(id);

        if (!existing.getIsSystem() &&
                !existing.getCreatedBy().getId().equals(currentUser.getId()) &&
                !securityUtils.isAdmin()) {
            throw new RuntimeException("Permission denied");
        }

        if (existing.getIsSystem()) {
            throw new RuntimeException("System templates cannot be modified");
        }

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getContent() != null) existing.setContent(updates.getContent());
        if (updates.getCategory() != null) existing.setCategory(updates.getCategory());

        ReportTemplate saved = templateRepository.save(existing);

        auditService.log(
                AuditService.ACTION_UPDATE,
                "REPORT_TEMPLATE",
                saved.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        ReportTemplate template = getTemplateById(id);

        if (!template.getCreatedBy().getId().equals(currentUser.getId()) &&
                !securityUtils.isAdmin()) {
            throw new RuntimeException("Permission denied");
        }

        if (template.getIsSystem()) {
            throw new RuntimeException("System templates cannot be deleted");
        }

        template.setIsActive(false);
        templateRepository.save(template);

        auditService.log(
                AuditService.ACTION_DELETE,
                "REPORT_TEMPLATE",
                template.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );
    }
}