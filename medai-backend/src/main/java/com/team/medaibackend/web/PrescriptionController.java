package com.team.medaibackend.web;

import com.team.medaibackend.entity.Medication;
import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.entity.Prescription;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.MedicationRepository;
import com.team.medaibackend.repository.PatientRepository;
import com.team.medaibackend.repository.PrescriptionRepository;
import com.team.medaibackend.repository.UserRepository;
import com.team.medaibackend.security.SecurityUtils;
import com.team.medaibackend.service.AuditService;
import com.team.medaibackend.service.PrescriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PrescriptionService prescriptionService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public PrescriptionController(
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            PrescriptionService prescriptionService,
            SecurityUtils securityUtils,
            AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.prescriptionService = prescriptionService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    // ============================================================================
    // MEDICATION SEARCH (for autocomplete)
    // ============================================================================

    @GetMapping("/medications/search")
    public ResponseEntity<?> searchMedications(@RequestParam String query) {
        try {
            List<Medication> medications = medicationRepository.searchForAutocomplete(query);

            List<Map<String, Object>> results = medications.stream()
                    .limit(10) // Top 10 results
                    .map(med -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", med.getId());
                        dto.put("name", med.getName());
                        dto.put("genericName", med.getGenericName());
                        dto.put("brandName", med.getBrandName());
                        dto.put("drugClass", med.getDrugClass());
                        dto.put("defaultDosage", med.getDefaultDosage());
                        dto.put("dosageForms", med.getDosageForms());
                        dto.put("strengths", med.getStrengths());
                        dto.put("routes", med.getRoutes());
                        dto.put("description", med.getDescription());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/medications/{id}")
    public ResponseEntity<?> getMedicationDetail(@PathVariable Long id) {
        Medication med = medicationRepository.findById(id).orElse(null);
        if (med == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", med.getId());
        dto.put("name", med.getName());
        dto.put("genericName", med.getGenericName());
        dto.put("brandName", med.getBrandName());
        dto.put("drugClass", med.getDrugClass());
        dto.put("description", med.getDescription());
        dto.put("indications", med.getIndications());
        dto.put("contraindications", med.getContraindications());
        dto.put("sideEffects", med.getSideEffects());
        dto.put("warnings", med.getWarnings());
        dto.put("defaultDosage", med.getDefaultDosage());
        dto.put("dosageForms", med.getDosageForms());
        dto.put("strengths", med.getStrengths());
        dto.put("routes", med.getRoutes());
        dto.put("interactionWarnings", med.getInteractionWarnings());
        dto.put("foodInteractions", med.getFoodInteractions());
        dto.put("alcoholWarning", med.getAlcoholWarning());
        dto.put("pregnancyCategory", med.getPregnancyCategory());
        dto.put("minAge", med.getMinAge());
        dto.put("maxAge", med.getMaxAge());
        dto.put("controlledSubstance", med.getControlledSubstance());
        dto.put("scheduleClass", med.getScheduleClass());

        return ResponseEntity.ok(dto);
    }

    // ============================================================================
    // DRUG INTERACTION CHECK
    // ============================================================================

    @PostMapping("/check-interactions")
    public ResponseEntity<?> checkInteractions(@RequestBody Map<String, Object> request) {
        try {
            Long patientId = Long.valueOf(request.get("patientId").toString());
            Long medicationId = request.get("medicationId") != null
                    ? Long.valueOf(request.get("medicationId").toString()) : null;
            String medicationName = (String) request.get("medicationName");

            Map<String, Object> result = prescriptionService.checkDrugInteractions(
                    patientId, medicationId, medicationName);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // DOSAGE CALCULATOR
    // ============================================================================

    @PostMapping("/calculate-dosage")
    public ResponseEntity<?> calculateDosage(@RequestBody Map<String, Object> request) {
        try {
            Long patientId = Long.valueOf(request.get("patientId").toString());
            Long medicationId = Long.valueOf(request.get("medicationId").toString());

            Map<String, Object> result = prescriptionService.calculateDosage(patientId, medicationId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // CREATE PRESCRIPTION
    // ============================================================================

    @PostMapping
    public ResponseEntity<?> createPrescription(@RequestBody Map<String, Object> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        // Verify doctor role
        if (!"DOCTOR".equals(doctor.getPrimaryRole()) && !securityUtils.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Only doctors can create prescriptions"));
        }

        try {
            // Get patient
            Long patientId = Long.valueOf(request.get("patientId").toString());
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            // Create prescription
            Prescription rx = new Prescription();
            rx.setPatient(patient);
            rx.setDoctor(doctor);

            // Medication
            if (request.containsKey("medicationId") && request.get("medicationId") != null) {
                Long medId = Long.valueOf(request.get("medicationId").toString());
                Medication medication = medicationRepository.findById(medId).orElse(null);
                rx.setMedication(medication);
                if (medication != null) {
                    rx.setMedicationName(medication.getName());
                }
            } else {
                rx.setMedicationName((String) request.get("medicationName"));
            }

            // Required fields
            rx.setDosage((String) request.get("dosage"));
            rx.setDosageForm((String) request.get("dosageForm"));
            rx.setRoute((String) request.get("route"));
            rx.setFrequency((String) request.get("frequency"));
            rx.setQuantity(Integer.valueOf(request.get("quantity").toString()));
            rx.setInstructions((String) request.get("instructions"));

            // Optional fields
            if (request.containsKey("frequencyCode"))
                rx.setFrequencyCode((String) request.get("frequencyCode"));
            if (request.containsKey("durationDays"))
                rx.setDurationDays(Integer.valueOf(request.get("durationDays").toString()));
            if (request.containsKey("refillsAllowed")) {
                int refills = Integer.parseInt(request.get("refillsAllowed").toString());
                rx.setRefillsAllowed(refills);
                rx.setRefillsRemaining(refills);
            }
            if (request.containsKey("patientInstructions"))
                rx.setPatientInstructions((String) request.get("patientInstructions"));
            if (request.containsKey("notes"))
                rx.setNotes((String) request.get("notes"));

            // Timing
            if (request.containsKey("timingMorning"))
                rx.setTimingMorning((Boolean) request.get("timingMorning"));
            if (request.containsKey("timingAfternoon"))
                rx.setTimingAfternoon((Boolean) request.get("timingAfternoon"));
            if (request.containsKey("timingEvening"))
                rx.setTimingEvening((Boolean) request.get("timingEvening"));
            if (request.containsKey("timingBedtime"))
                rx.setTimingBedtime((Boolean) request.get("timingBedtime"));
            if (request.containsKey("timingAsNeeded"))
                rx.setTimingAsNeeded((Boolean) request.get("timingAsNeeded"));

            // Dates
            rx.setStartDate(request.containsKey("startDate")
                    ? LocalDate.parse(request.get("startDate").toString())
                    : LocalDate.now());

            if (request.containsKey("endDate")) {
                rx.setEndDate(LocalDate.parse(request.get("endDate").toString()));
            } else if (rx.getDurationDays() != null) {
                rx.setEndDate(prescriptionService.calculateEndDate(rx.getStartDate(), rx.getDurationDays()));
            }

            // Pharmacy info
            if (request.containsKey("pharmacyName"))
                rx.setPharmacyName((String) request.get("pharmacyName"));
            if (request.containsKey("pharmacyPhone"))
                rx.setPharmacyPhone((String) request.get("pharmacyPhone"));

            // Follow-up
            if (request.containsKey("requiresFollowup")) {
                rx.setRequiresFollowup((Boolean) request.get("requiresFollowup"));
                if (Boolean.TRUE.equals(rx.getRequiresFollowup()) && request.containsKey("followupDate")) {
                    rx.setFollowupDate(LocalDate.parse(request.get("followupDate").toString()));
                }
            }

            // Safety checks
            if (request.containsKey("interactionWarnings")) {
                rx.setInteractionWarnings((Map<String, Object>) request.get("interactionWarnings"));
            }
            if (request.containsKey("allergyOverride")) {
                rx.setAllergyOverride((Boolean) request.get("allergyOverride"));
                if (Boolean.TRUE.equals(rx.getAllergyOverride())) {
                    rx.setAllergyOverrideReason((String) request.get("allergyOverrideReason"));
                }
            }

            // Generate unique identifiers
            rx.setPrescriptionNumber(prescriptionService.generatePrescriptionNumber());
            rx.setVerificationCode(prescriptionService.generateVerificationCode());

            // Save
            Prescription saved = prescriptionRepository.save(rx);

            // Audit log
            auditService.log(
                    "PRESCRIPTION_CREATED",
                    "PRESCRIPTION",
                    saved.getId().toString(),
                    doctor.getId(),
                    doctor.getUsername()
            );

            return ResponseEntity.ok(buildPrescriptionDto(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // GET PATIENT PRESCRIPTIONS
    // ============================================================================

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientPrescriptions(@PathVariable Long patientId) {
        try {
            List<Prescription> prescriptions = prescriptionRepository
                    .findByPatientIdOrderByPrescribedDateDesc(patientId);

            List<Map<String, Object>> result = prescriptions.stream()
                    .map(this::buildPrescriptionDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<?> getActivePatientPrescriptions(@PathVariable Long patientId) {
        try {
            List<Prescription> prescriptions = prescriptionRepository
                    .findActiveByPatientId(patientId);

            List<Map<String, Object>> result = prescriptions.stream()
                    .map(this::buildPrescriptionDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // GET MY PRESCRIPTIONS (for patients)
    // ============================================================================

    @GetMapping("/my-prescriptions")
    public ResponseEntity<?> getMyPrescriptions(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Patient patient = patientRepository.findByUser_Email(user.getEmail()).orElse(null);
        if (patient == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Prescription> prescriptions = prescriptionRepository
                .findByPatientIdOrderByPrescribedDateDesc(patient.getId());

        List<Map<String, Object>> result = prescriptions.stream()
                .map(this::buildPrescriptionDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ============================================================================
    // GET PRESCRIPTION DETAIL
    // ============================================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getPrescriptionDetail(@PathVariable Long id) {
        Prescription rx = prescriptionRepository.findById(id).orElse(null);
        if (rx == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildPrescriptionDto(rx));
    }

    // ============================================================================
    // UPDATE PRESCRIPTION STATUS
    // ============================================================================

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = securityUtils.getCurrentUserOrThrow();

        Prescription rx = prescriptionRepository.findById(id).orElse(null);
        if (rx == null) {
            return ResponseEntity.notFound().build();
        }

        String newStatus = request.get("status");
        String oldStatus = rx.getStatus();
        rx.setStatus(newStatus);

        if ("cancelled".equals(newStatus)) {
            rx.setCancelledAt(java.time.LocalDateTime.now());
            rx.setCancelledBy(user.getId());
            rx.setCancellationReason(request.get("reason"));
        }

        prescriptionRepository.save(rx);

        auditService.log(
                "PRESCRIPTION_STATUS_UPDATED",
                "PRESCRIPTION",
                rx.getId().toString(),
                user.getId(),
                user.getUsername()
        );

        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }

    // ============================================================================
    // REFILL PRESCRIPTION
    // ============================================================================

    @PostMapping("/{id}/refill")
    public ResponseEntity<?> refillPrescription(@PathVariable Long id) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        Prescription rx = prescriptionRepository.findById(id).orElse(null);
        if (rx == null) {
            return ResponseEntity.notFound().build();
        }

        if (rx.getRefillsRemaining() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "No refills remaining"));
        }

        rx.setRefillsRemaining(rx.getRefillsRemaining() - 1);
        rx.setLastRefillDate(LocalDate.now());

        prescriptionRepository.save(rx);

        auditService.log(
                "PRESCRIPTION_REFILLED",
                "PRESCRIPTION",
                rx.getId().toString(),
                doctor.getId(),
                doctor.getUsername()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Refill processed",
                "refillsRemaining", rx.getRefillsRemaining()
        ));
    }

    // ============================================================================
    // HELPER METHOD: Build Prescription DTO
    // ============================================================================

    private Map<String, Object> buildPrescriptionDto(Prescription rx) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("id", rx.getId());
        dto.put("prescriptionNumber", rx.getPrescriptionNumber());
        dto.put("medicationName", rx.getMedicationName());
        dto.put("dosage", rx.getDosage());
        dto.put("dosageForm", rx.getDosageForm());
        dto.put("route", rx.getRoute());
        dto.put("frequency", rx.getFrequency());
        dto.put("frequencyCode", rx.getFrequencyCode());
        dto.put("quantity", rx.getQuantity());
        dto.put("durationDays", rx.getDurationDays());
        dto.put("instructions", rx.getInstructions());
        dto.put("patientInstructions", rx.getPatientInstructions());
        dto.put("notes", rx.getNotes());
        dto.put("status", rx.getStatus());
        dto.put("prescribedDate", rx.getPrescribedDate());
        dto.put("startDate", rx.getStartDate());
        dto.put("endDate", rx.getEndDate());
        dto.put("refillsAllowed", rx.getRefillsAllowed());
        dto.put("refillsRemaining", rx.getRefillsRemaining());
        dto.put("lastRefillDate", rx.getLastRefillDate());
        dto.put("verified", rx.getVerified());
        dto.put("verificationCode", rx.getVerificationCode());
        dto.put("interactionWarnings", rx.getInteractionWarnings());
        dto.put("pharmacyName", rx.getPharmacyName());
        dto.put("pharmacyPhone", rx.getPharmacyPhone());
        dto.put("requiresFollowup", rx.getRequiresFollowup());
        dto.put("followupDate", rx.getFollowupDate());
        dto.put("createdAt", rx.getCreatedAt());

        // Timing
        dto.put("timingMorning", rx.getTimingMorning());
        dto.put("timingAfternoon", rx.getTimingAfternoon());
        dto.put("timingEvening", rx.getTimingEvening());
        dto.put("timingBedtime", rx.getTimingBedtime());
        dto.put("timingAsNeeded", rx.getTimingAsNeeded());

        // Patient info
        if (rx.getPatient() != null) {
            dto.put("patientId", rx.getPatient().getId());
            dto.put("patientName", rx.getPatient().getName());
            dto.put("patientIdNumber", rx.getPatient().getPatientId());
        }

        // Doctor info
        if (rx.getDoctor() != null) {
            dto.put("doctorId", rx.getDoctor().getId());
            dto.put("doctorName", rx.getDoctor().getFullName());
        }

        // Medication details
        if (rx.getMedication() != null) {
            dto.put("medicationId", rx.getMedication().getId());
            dto.put("genericName", rx.getMedication().getGenericName());
            dto.put("brandName", rx.getMedication().getBrandName());
            dto.put("drugClass", rx.getMedication().getDrugClass());
        }

        return dto;
    }
}