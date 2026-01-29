package com.team.medaibackend.service;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TreatmentPlanService {

    private final TreatmentPlanRepository planRepository;
    private final TreatmentStepRepository stepRepository;
    private final TreatmentPlanTemplateRepository templateRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public TreatmentPlanService(
            TreatmentPlanRepository planRepository,
            TreatmentStepRepository stepRepository,
            TreatmentPlanTemplateRepository templateRepository,
            PatientRepository patientRepository,
            AuditService auditService) {
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.templateRepository = templateRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    /**
     * Generate unique plan UID
     * Format: PLAN-YYYYMMDD-XXXXXX
     */
    public String generatePlanUid(LocalDate startDate) {
        String datePrefix = "PLAN-" + startDate.toString().replace("-", "");
        long count = planRepository.count();
        int sequence = (int) (count % 1000000) + 1;
        return String.format("%s-%06d", datePrefix, sequence);
    }

    /**
     * Apply template to create plan
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> applyTemplate(Long templateId) {
        TreatmentPlanTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template == null) {
            return null;
        }

        Map<String, Object> templateData = template.getTemplateData();
        Map<String, Object> planData = new HashMap<>();

        // Extract plan details
        planData.put("title", templateData.get("title"));
        planData.put("category", template.getCategory());
        planData.put("description", template.getDescription());

        // Calculate dates based on duration
        if (templateData.containsKey("duration_days")) {
            int duration = ((Number) templateData.get("duration_days")).intValue();
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(duration);
            planData.put("startDate", startDate);
            planData.put("expectedCompletionDate", endDate);
        }

        // Extract steps
        if (templateData.containsKey("steps")) {
            List<Map<String, Object>> templateSteps = (List<Map<String, Object>>) templateData.get("steps");
            List<Map<String, Object>> steps = new ArrayList<>();

            LocalDate currentDate = LocalDate.now();

            for (Map<String, Object> templateStep : templateSteps) {
                Map<String, Object> step = new HashMap<>();
                step.put("order", templateStep.get("order"));
                step.put("title", templateStep.get("title"));
                step.put("description", templateStep.get("description"));
                step.put("instructions", templateStep.get("instructions"));
                step.put("durationDays", templateStep.get("duration_days"));

                // Calculate due date
                if (templateStep.containsKey("duration_days")) {
                    int duration = ((Number) templateStep.get("duration_days")).intValue();
                    step.put("dueDate", currentDate.plusDays(duration));
                }

                steps.add(step);
            }

            planData.put("steps", steps);
        }

        // Increment usage count
        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);

        return planData;
    }

    /**
     * Complete a step and update plan progress
     */
    @Transactional
    public TreatmentStep completeStep(Long stepId, String completionNotes, User completedBy) {
        TreatmentStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));

        if ("completed".equals(step.getStatus())) {
            throw new RuntimeException("Step already completed");
        }

        step.setStatus("completed");
        step.setCompletedAt(LocalDateTime.now());
        step.setCompletionNotes(completionNotes);

        TreatmentStep saved = stepRepository.save(step);

        // Progress will be auto-updated by database trigger

        // Audit log
        auditService.log(
                "TREATMENT_STEP_COMPLETED",
                "TREATMENT_STEP",
                saved.getId().toString(),
                completedBy.getId(),
                completedBy.getUsername()
        );

        return saved;
    }

    /**
     * Skip a step
     */
    @Transactional
    public TreatmentStep skipStep(Long stepId, String reason, User user) {
        TreatmentStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));

        step.setStatus("skipped");
        step.setCompletionNotes("Skipped: " + reason);

        TreatmentStep saved = stepRepository.save(step);

        // Audit log
        auditService.log(
                "TREATMENT_STEP_SKIPPED",
                "TREATMENT_STEP",
                saved.getId().toString(),
                user.getId(),
                user.getUsername()
        );

        return saved;
    }

    /**
     * Cancel a treatment plan
     */
    @Transactional
    public TreatmentPlan cancelPlan(Long planId, String reason, User user) {
        TreatmentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        if ("completed".equals(plan.getStatus()) || "cancelled".equals(plan.getStatus())) {
            throw new RuntimeException("Cannot cancel completed or already cancelled plan");
        }

        plan.setStatus("cancelled");
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") +
                "Cancelled: " + reason + " (by " + user.getFullName() + " on " + LocalDate.now() + ")");

        TreatmentPlan saved = planRepository.save(plan);

        // Audit log
        auditService.log(
                "TREATMENT_PLAN_CANCELLED",
                "TREATMENT_PLAN",
                saved.getId().toString(),
                user.getId(),
                user.getUsername()
        );

        return saved;
    }

    /**
     * Hold a treatment plan
     */
    @Transactional
    public TreatmentPlan holdPlan(Long planId, String reason, User user) {
        TreatmentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        plan.setStatus("on_hold");
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") +
                "Put on hold: " + reason + " (by " + user.getFullName() + " on " + LocalDate.now() + ")");

        TreatmentPlan saved = planRepository.save(plan);

        auditService.log(
                "TREATMENT_PLAN_HELD",
                "TREATMENT_PLAN",
                saved.getId().toString(),
                user.getId(),
                user.getUsername()
        );

        return saved;
    }

    /**
     * Resume a held plan
     */
    @Transactional
    public TreatmentPlan resumePlan(Long planId, User user) {
        TreatmentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        if (!"on_hold".equals(plan.getStatus())) {
            throw new RuntimeException("Only plans on hold can be resumed");
        }

        plan.setStatus("active");
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") +
                "Resumed (by " + user.getFullName() + " on " + LocalDate.now() + ")");

        TreatmentPlan saved = planRepository.save(plan);

        auditService.log(
                "TREATMENT_PLAN_RESUMED",
                "TREATMENT_PLAN",
                saved.getId().toString(),
                user.getId(),
                user.getUsername()
        );

        return saved;
    }

    /**
     * Get plan statistics for patient
     */
    public Map<String, Object> getPatientPlanStats(Long patientId) {
        Map<String, Object> stats = new HashMap<>();

        List<TreatmentPlan> allPlans = planRepository.findByPatientIdOrderByStartDateDesc(patientId);

        long activePlans = allPlans.stream().filter(p -> "active".equals(p.getStatus())).count();
        long completedPlans = allPlans.stream().filter(p -> "completed".equals(p.getStatus())).count();
        long cancelledPlans = allPlans.stream().filter(p -> "cancelled".equals(p.getStatus())).count();

        stats.put("totalPlans", allPlans.size());
        stats.put("activePlans", activePlans);
        stats.put("completedPlans", completedPlans);
        stats.put("cancelledPlans", cancelledPlans);

        // Average progress of active plans
        OptionalDouble avgProgress = allPlans.stream()
                .filter(p -> "active".equals(p.getStatus()))
                .mapToInt(TreatmentPlan::getProgressPercentage)
                .average();

        stats.put("averageProgress", avgProgress.isPresent() ? (int) avgProgress.getAsDouble() : 0);

        return stats;
    }

    /**
     * Get upcoming deadlines for patient
     */
    public List<Map<String, Object>> getUpcomingDeadlines(Long patientId, int days) {
        List<Map<String, Object>> deadlines = new ArrayList<>();

        List<TreatmentPlan> activePlans = planRepository.findActiveByPatient(patientId);
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);

        for (TreatmentPlan plan : activePlans) {
            List<TreatmentStep> steps = stepRepository.findStepsDueSoon(plan.getId(), today, futureDate);

            for (TreatmentStep step : steps) {
                Map<String, Object> deadline = new HashMap<>();
                deadline.put("planId", plan.getId());
                deadline.put("planTitle", plan.getTitle());
                deadline.put("stepId", step.getId());
                deadline.put("stepTitle", step.getTitle());
                deadline.put("dueDate", step.getDueDate());
                deadline.put("daysRemaining", java.time.temporal.ChronoUnit.DAYS.between(today, step.getDueDate()));

                deadlines.add(deadline);
            }
        }

        // Sort by due date
        deadlines.sort((a, b) -> ((LocalDate) a.get("dueDate")).compareTo((LocalDate) b.get("dueDate")));

        return deadlines;
    }

    /**
     * Validate plan data
     */
    public Map<String, String> validatePlan(TreatmentPlan plan) {
        Map<String, String> errors = new HashMap<>();

        if (plan.getTitle() == null || plan.getTitle().trim().isEmpty()) {
            errors.put("title", "Title is required");
        }

        if (plan.getStartDate() == null) {
            errors.put("startDate", "Start date is required");
        }

        if (plan.getPatient() == null) {
            errors.put("patient", "Patient is required");
        }

        if (plan.getDoctor() == null) {
            errors.put("doctor", "Doctor is required");
        }

        if (plan.getExpectedCompletionDate() != null && plan.getStartDate() != null) {
            if (plan.getExpectedCompletionDate().isBefore(plan.getStartDate())) {
                errors.put("expectedCompletionDate", "Expected completion date cannot be before start date");
            }
        }

        return errors;
    }
}