package com.team.medaibackend.web;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import com.team.medaibackend.security.SecurityUtils;
import com.team.medaibackend.service.AuditService;
import com.team.medaibackend.service.TreatmentPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/treatment-plans")
public class TreatmentPlanController {

    private final TreatmentPlanRepository planRepository;
    private final TreatmentStepRepository stepRepository;
    private final TreatmentPlanTemplateRepository templateRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final TreatmentPlanService treatmentPlanService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public TreatmentPlanController(
            TreatmentPlanRepository planRepository,
            TreatmentStepRepository stepRepository,
            TreatmentPlanTemplateRepository templateRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            TreatmentPlanService treatmentPlanService,
            SecurityUtils securityUtils,
            AuditService auditService) {
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.templateRepository = templateRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.treatmentPlanService = treatmentPlanService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    // ========================================================================
    // TREATMENT PLANS CRUD
    // ========================================================================

    @GetMapping
    public ResponseEntity<?> getAllPlans(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category
    ) {
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            List<TreatmentPlan> plans;

            if (patientId != null) {
                if (status != null) {
                    plans = planRepository.findByPatientIdAndStatusOrderByStartDateDesc(patientId, status);
                } else {
                    plans = planRepository.findByPatientIdOrderByStartDateDesc(patientId);
                }
            } else if (priority != null) {
                plans = planRepository.findByPriorityOrderByStartDateDesc(priority);
            } else if (category != null) {
                plans = planRepository.findByCategoryOrderByStartDateDesc(category);
            } else if ("DOCTOR".equals(currentUser.getPrimaryRole())) {
                plans = planRepository.findByDoctorIdOrderByStartDateDesc(currentUser.getId());
            } else {
                plans = planRepository.findAll();
            }

            List<Map<String, Object>> result = plans.stream()
                    .map(this::buildPlanDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody Map<String, Object> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        if (!"DOCTOR".equals(doctor.getPrimaryRole()) && !securityUtils.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Only doctors can create treatment plans"));
        }

        try {
            if (request.get("patientId") == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Patient ID is required"));
            }

            Long patientId = Long.valueOf(request.get("patientId").toString());
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            TreatmentPlan plan = new TreatmentPlan();
            plan.setPatient(patient);
            plan.setDoctor(doctor);
            plan.setTitle((String) request.get("title"));
            plan.setDiagnosis((String) request.get("diagnosis"));
            plan.setGoals((String) request.get("goals"));
            plan.setDescription((String) request.get("description"));
            plan.setPriority(request.getOrDefault("priority", "medium").toString());
            plan.setCategory((String) request.get("category"));

            if (request.containsKey("startDate")) {
                plan.setStartDate(LocalDate.parse(request.get("startDate").toString()));
            } else {
                plan.setStartDate(LocalDate.now());
            }

            if (request.containsKey("expectedCompletionDate")) {
                plan.setExpectedCompletionDate(LocalDate.parse(request.get("expectedCompletionDate").toString()));
            }

            // Validate
            Map<String, String> errors = treatmentPlanService.validatePlan(plan);
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("errors", errors));
            }

            // Generate UID
            plan.setPlanUid(treatmentPlanService.generatePlanUid(plan.getStartDate()));

            // Save plan
            TreatmentPlan savedPlan = planRepository.save(plan);

            // Create steps if provided
            if (request.containsKey("steps")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stepsData = (List<Map<String, Object>>) request.get("steps");

                for (Map<String, Object> stepData : stepsData) {
                    TreatmentStep step = new TreatmentStep();
                    step.setPlan(savedPlan);
                    step.setStepOrder(Integer.valueOf(stepData.get("order").toString()));
                    step.setTitle((String) stepData.get("title"));
                    step.setDescription((String) stepData.get("description"));
                    step.setInstructions((String) stepData.get("instructions"));

                    if (stepData.containsKey("durationDays")) {
                        step.setDurationDays(Integer.valueOf(stepData.get("durationDays").toString()));
                    }

                    if (stepData.containsKey("dueDate")) {
                        step.setDueDate(LocalDate.parse(stepData.get("dueDate").toString()));
                    }

                    stepRepository.save(step);
                }
            }

            auditService.log(
                    "TREATMENT_PLAN_CREATED",
                    "TREATMENT_PLAN",
                    savedPlan.getId().toString(),
                    doctor.getId(),
                    doctor.getUsername()
            );

            return ResponseEntity.ok(buildPlanDto(savedPlan));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanDetail(@PathVariable Long id) {
        try {
            TreatmentPlan plan = planRepository.findById(id).orElse(null);
            if (plan == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> result = buildPlanDto(plan);

            // Include steps
            List<TreatmentStep> steps = stepRepository.findByPlanIdOrderByStepOrderAsc(id);
            result.put("steps", steps.stream().map(this::buildStepDto).collect(Collectors.toList()));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            TreatmentPlan plan = planRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            if (!plan.getDoctor().getId().equals(doctor.getId()) && !securityUtils.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of("message", "You can only edit your own plans"));
            }

            if (request.containsKey("title")) plan.setTitle((String) request.get("title"));
            if (request.containsKey("diagnosis")) plan.setDiagnosis((String) request.get("diagnosis"));
            if (request.containsKey("goals")) plan.setGoals((String) request.get("goals"));
            if (request.containsKey("description")) plan.setDescription((String) request.get("description"));
            if (request.containsKey("priority")) plan.setPriority((String) request.get("priority"));
            if (request.containsKey("category")) plan.setCategory((String) request.get("category"));
            if (request.containsKey("notes")) plan.setNotes((String) request.get("notes"));

            if (request.containsKey("expectedCompletionDate")) {
                plan.setExpectedCompletionDate(LocalDate.parse(request.get("expectedCompletionDate").toString()));
            }

            TreatmentPlan saved = planRepository.save(plan);

            auditService.log(
                    "TREATMENT_PLAN_UPDATED",
                    "TREATMENT_PLAN",
                    saved.getId().toString(),
                    doctor.getId(),
                    doctor.getUsername()
            );

            return ResponseEntity.ok(buildPlanDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            TreatmentPlan plan = planRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            if (!plan.getDoctor().getId().equals(doctor.getId()) && !securityUtils.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of("message", "You can only delete your own plans"));
            }

            if ("completed".equals(plan.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete completed plans"));
            }

            planRepository.delete(plan);

            auditService.log(
                    "TREATMENT_PLAN_DELETED",
                    "TREATMENT_PLAN",
                    id.toString(),
                    doctor.getId(),
                    doctor.getUsername()
            );

            return ResponseEntity.ok(Map.of("message", "Plan deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ========================================================================
    // PLAN ACTIONS
    // ========================================================================

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPlan(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cancellation reason is required"));
            }

            TreatmentPlan cancelled = treatmentPlanService.cancelPlan(id, reason, doctor);
            return ResponseEntity.ok(buildPlanDto(cancelled));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/hold")
    public ResponseEntity<?> holdPlan(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            String reason = request.get("reason");
            TreatmentPlan held = treatmentPlanService.holdPlan(id, reason, doctor);
            return ResponseEntity.ok(buildPlanDto(held));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumePlan(@PathVariable Long id) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            TreatmentPlan resumed = treatmentPlanService.resumePlan(id, doctor);
            return ResponseEntity.ok(buildPlanDto(resumed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ========================================================================
    // TREATMENT STEPS
    // ========================================================================

    @GetMapping("/{planId}/steps")
    public ResponseEntity<?> getPlanSteps(@PathVariable Long planId) {
        try {
            List<TreatmentStep> steps = stepRepository.findByPlanIdOrderByStepOrderAsc(planId);
            List<Map<String, Object>> result = steps.stream()
                    .map(this::buildStepDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{planId}/steps")
    public ResponseEntity<?> addStep(@PathVariable Long planId, @RequestBody Map<String, Object> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        try {
            TreatmentPlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            TreatmentStep step = new TreatmentStep();
            step.setPlan(plan);
            step.setStepOrder(Integer.valueOf(request.get("order").toString()));
            step.setTitle((String) request.get("title"));
            step.setDescription((String) request.get("description"));
            step.setInstructions((String) request.get("instructions"));

            if (request.containsKey("durationDays")) {
                step.setDurationDays(Integer.valueOf(request.get("durationDays").toString()));
            }

            if (request.containsKey("dueDate")) {
                step.setDueDate(LocalDate.parse(request.get("dueDate").toString()));
            }

            if (request.containsKey("assignedToId")) {
                Long assignedToId = Long.valueOf(request.get("assignedToId").toString());
                User assignedUser = userRepository.findById(assignedToId).orElse(null);
                step.setAssignedTo(assignedUser);
            }

            TreatmentStep saved = stepRepository.save(step);

            auditService.log(
                    "TREATMENT_STEP_ADDED",
                    "TREATMENT_STEP",
                    saved.getId().toString(),
                    doctor.getId(),
                    doctor.getUsername()
            );

            return ResponseEntity.ok(buildStepDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/steps/{stepId}")
    public ResponseEntity<?> updateStep(@PathVariable Long stepId, @RequestBody Map<String, Object> request) {
        User user = securityUtils.getCurrentUserOrThrow();

        try {
            TreatmentStep step = stepRepository.findById(stepId)
                    .orElseThrow(() -> new RuntimeException("Step not found"));

            if (request.containsKey("title")) step.setTitle((String) request.get("title"));
            if (request.containsKey("description")) step.setDescription((String) request.get("description"));
            if (request.containsKey("instructions")) step.setInstructions((String) request.get("instructions"));
            if (request.containsKey("status")) step.setStatus((String) request.get("status"));
            if (request.containsKey("notes")) step.setNotes((String) request.get("notes"));

            if (request.containsKey("dueDate")) {
                step.setDueDate(LocalDate.parse(request.get("dueDate").toString()));
            }

            TreatmentStep saved = stepRepository.save(step);
            return ResponseEntity.ok(buildStepDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/steps/{stepId}/complete")
    public ResponseEntity<?> completeStep(@PathVariable Long stepId, @RequestBody Map<String, String> request) {
        User user = securityUtils.getCurrentUserOrThrow();

        try {
            String notes = request.get("completionNotes");
            TreatmentStep completed = treatmentPlanService.completeStep(stepId, notes, user);
            return ResponseEntity.ok(buildStepDto(completed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/steps/{stepId}/skip")
    public ResponseEntity<?> skipStep(@PathVariable Long stepId, @RequestBody Map<String, String> request) {
        User user = securityUtils.getCurrentUserOrThrow();

        try {
            String reason = request.get("reason");
            TreatmentStep skipped = treatmentPlanService.skipStep(stepId, reason, user);
            return ResponseEntity.ok(buildStepDto(skipped));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<?> deleteStep(@PathVariable Long stepId) {
        User user = securityUtils.getCurrentUserOrThrow();

        try {
            TreatmentStep step = stepRepository.findById(stepId)
                    .orElseThrow(() -> new RuntimeException("Step not found"));

            stepRepository.delete(step);

            auditService.log(
                    "TREATMENT_STEP_DELETED",
                    "TREATMENT_STEP",
                    stepId.toString(),
                    user.getId(),
                    user.getUsername()
            );

            return ResponseEntity.ok(Map.of("message", "Step deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ========================================================================
    // TEMPLATES
    // ========================================================================

    @GetMapping("/templates")
    public ResponseEntity<?> getAllTemplates(@RequestParam(required = false) String category) {
        try {
            List<TreatmentPlanTemplate> templates;

            if (category != null) {
                templates = templateRepository.findByCategoryOrderByUsageCountDesc(category);
            } else {
                templates = templateRepository.findByIsSystemTrueOrderByUsageCountDesc();
            }

            List<Map<String, Object>> result = templates.stream()
                    .map(this::buildTemplateDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        TreatmentPlanTemplate template = templateRepository.findById(id).orElse(null);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildTemplateDto(template));
    }

    @PostMapping("/templates/{id}/apply")
    public ResponseEntity<?> applyTemplate(@PathVariable Long id) {
        try {
            Map<String, Object> planData = treatmentPlanService.applyTemplate(id);
            if (planData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(planData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ========================================================================
    // PATIENT-SPECIFIC ENDPOINTS
    // ========================================================================

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientPlans(@PathVariable Long patientId) {
        try {
            List<TreatmentPlan> plans = planRepository.findByPatientIdOrderByStartDateDesc(patientId);
            List<Map<String, Object>> result = plans.stream()
                    .map(this::buildPlanDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}/stats")
    public ResponseEntity<?> getPatientPlanStats(@PathVariable(required = false) Long patientId) {
        // Validate patient ID
        if (patientId == null || patientId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "Invalid patient ID",
                            "error", "Patient ID must be a valid positive number"
                    ));
        }

        try {
            Map<String, Object> stats = treatmentPlanService.getPatientPlanStats(patientId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}/deadlines")
    public ResponseEntity<?> getUpcomingDeadlines(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "7") int days
    ) {
        try {
            List<Map<String, Object>> deadlines = treatmentPlanService.getUpcomingDeadlines(patientId, days);
            return ResponseEntity.ok(deadlines);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Map<String, Object> buildPlanDto(TreatmentPlan plan) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("id", plan.getId());
        dto.put("planUid", plan.getPlanUid());
        dto.put("title", plan.getTitle());
        dto.put("diagnosis", plan.getDiagnosis());
        dto.put("goals", plan.getGoals());
        dto.put("description", plan.getDescription());
        dto.put("startDate", plan.getStartDate());
        dto.put("endDate", plan.getEndDate());
        dto.put("expectedCompletionDate", plan.getExpectedCompletionDate());
        dto.put("actualCompletionDate", plan.getActualCompletionDate());
        dto.put("status", plan.getStatus());
        dto.put("progressPercentage", plan.getProgressPercentage());
        dto.put("priority", plan.getPriority());
        dto.put("category", plan.getCategory());
        dto.put("notes", plan.getNotes());
        dto.put("createdAt", plan.getCreatedAt());
        dto.put("updatedAt", plan.getUpdatedAt());

        if (plan.getPatient() != null) {
            dto.put("patientId", plan.getPatient().getId());
            dto.put("patientName", plan.getPatient().getName());
            dto.put("patientIdNumber", plan.getPatient().getPatientId());
        }

        if (plan.getDoctor() != null) {
            dto.put("doctorId", plan.getDoctor().getId());
            dto.put("doctorName", plan.getDoctor().getFullName());
        }

        return dto;
    }

    private Map<String, Object> buildStepDto(TreatmentStep step) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("id", step.getId());
        dto.put("stepOrder", step.getStepOrder());
        dto.put("title", step.getTitle());
        dto.put("description", step.getDescription());
        dto.put("instructions", step.getInstructions());
        dto.put("durationDays", step.getDurationDays());
        dto.put("dueDate", step.getDueDate());
        dto.put("status", step.getStatus());
        dto.put("completedAt", step.getCompletedAt());
        dto.put("notes", step.getNotes());
        dto.put("completionNotes", step.getCompletionNotes());
        dto.put("createdAt", step.getCreatedAt());
        dto.put("updatedAt", step.getUpdatedAt());

        if (step.getAssignedTo() != null) {
            dto.put("assignedToId", step.getAssignedTo().getId());
            dto.put("assignedToName", step.getAssignedTo().getFullName());
        }

        return dto;
    }

    private Map<String, Object> buildTemplateDto(TreatmentPlanTemplate template) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("id", template.getId());
        dto.put("name", template.getName());
        dto.put("category", template.getCategory());
        dto.put("description", template.getDescription());
        dto.put("templateData", template.getTemplateData());
        dto.put("isSystem", template.getIsSystem());
        dto.put("specialty", template.getSpecialty());
        dto.put("usageCount", template.getUsageCount());
        dto.put("createdAt", template.getCreatedAt());

        if (template.getCreatedBy() != null) {
            dto.put("createdBy", template.getCreatedBy().getFullName());
        }

        return dto;
    }
}