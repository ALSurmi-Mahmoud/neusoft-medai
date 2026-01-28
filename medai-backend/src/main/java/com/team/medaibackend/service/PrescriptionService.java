package com.team.medaibackend.service;

import com.team.medaibackend.entity.Medication;
import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.entity.Prescription;
import com.team.medaibackend.repository.MedicationRepository;
import com.team.medaibackend.repository.PatientRepository;
import com.team.medaibackend.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            PatientRepository patientRepository,
            AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    /**
     * Generate unique prescription number
     * Format: RX-YYYYMMDD-XXXX
     */
    public String generatePrescriptionNumber() {
        String datePrefix = "RX-" + LocalDate.now().toString().replace("-", "");

        // Find highest number today
        String pattern = datePrefix + "%";
        long count = prescriptionRepository.count();

        // Generate sequential number
        int sequence = (int) (count % 10000) + 1;
        return String.format("%s-%04d", datePrefix, sequence);
    }

    /**
     * Generate 6-digit verification code
     */
    public String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    /**
     * Calculate end date based on duration
     */
    public LocalDate calculateEndDate(LocalDate startDate, Integer durationDays) {
        if (durationDays == null || durationDays <= 0) {
            // Default to 30 days if not specified
            return startDate.plusDays(30);
        }
        return startDate.plusDays(durationDays);
    }

    /**
     * SMART DRUG INTERACTION CHECKER
     * Checks for:
     * - Patient allergies
     * - Drug-drug interactions
     * - Age contraindications
     * - Condition contraindications
     * - Pregnancy warnings
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkDrugInteractions(Long patientId, Long medicationId, String medicationName) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> warnings = new ArrayList<>();
        boolean hasCritical = false;
        boolean hasWarning = false;

        // Get patient info
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) {
            result.put("error", "Patient not found");
            return result;
        }

        // Get medication info
        Medication medication = null;
        if (medicationId != null) {
            medication = medicationRepository.findById(medicationId).orElse(null);
        }

        // Calculate patient age
        Integer patientAge = null;
        if (patient.getBirthDate() != null) {
            patientAge = Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
        }

        // 1. CHECK ALLERGIES
        if (patient.getAllergies() != null && !patient.getAllergies().trim().isEmpty()) {
            String allergies = patient.getAllergies().toLowerCase();
            String drugNameToCheck = medication != null ? medication.getName().toLowerCase() : medicationName.toLowerCase();
            String genericName = medication != null && medication.getGenericName() != null
                    ? medication.getGenericName().toLowerCase() : "";

            if (allergies.contains(drugNameToCheck) ||
                    (genericName != null && allergies.contains(genericName)) ||
                    (medication != null && medication.getDrugClass() != null &&
                            allergies.contains(medication.getDrugClass().toLowerCase()))) {

                warnings.add(Map.of(
                        "level", "CRITICAL",
                        "type", "ALLERGY",
                        "message", "⚠️ PATIENT ALLERGIC TO THIS MEDICATION",
                        "detail", "Patient allergies: " + patient.getAllergies(),
                        "action", "Do not prescribe unless life-threatening situation with documented override"
                ));
                hasCritical = true;
            }
        }

        // 2. CHECK AGE RESTRICTIONS
        if (medication != null && patientAge != null) {
            if (medication.getMinAge() != null && patientAge < medication.getMinAge()) {
                warnings.add(Map.of(
                        "level", "CRITICAL",
                        "type", "AGE_RESTRICTION",
                        "message", "⚠️ PATIENT TOO YOUNG FOR THIS MEDICATION",
                        "detail", String.format("Minimum age: %d years, Patient age: %d years",
                                medication.getMinAge(), patientAge),
                        "action", "Consider age-appropriate alternative"
                ));
                hasCritical = true;
            }

            if (medication.getMaxAge() != null && patientAge > medication.getMaxAge()) {
                warnings.add(Map.of(
                        "level", "WARNING",
                        "type", "AGE_RESTRICTION",
                        "message", "⚠️ Caution: Patient older than recommended maximum age",
                        "detail", String.format("Maximum age: %d years, Patient age: %d years",
                                medication.getMaxAge(), patientAge),
                        "action", "Consider dose adjustment or alternative"
                ));
                hasWarning = true;
            }
        }

        // 3. CHECK DRUG-DRUG INTERACTIONS
        List<Prescription> activeRx = prescriptionRepository.findActiveByPatientId(patientId);
        if (!activeRx.isEmpty() && medication != null && medication.getInteractionWarnings() != null) {
            for (Prescription rx : activeRx) {
                String currentMedName = rx.getMedicationName() != null
                        ? rx.getMedicationName().toLowerCase()
                        : (rx.getMedication() != null ? rx.getMedication().getName().toLowerCase() : "");

                for (String interactingDrug : medication.getInteractionWarnings()) {
                    if (currentMedName.contains(interactingDrug.toLowerCase())) {
                        warnings.add(Map.of(
                                "level", "WARNING",
                                "type", "DRUG_INTERACTION",
                                "message", "⚠️ Drug Interaction Detected",
                                "detail", String.format("%s may interact with %s",
                                        medication.getName(), rx.getMedicationName()),
                                "action", "Review interaction severity and consider alternative or monitoring"
                        ));
                        hasWarning = true;
                    }
                }
            }
        }

        // 4. CHECK MEDICAL CONDITIONS
        if (medication != null && patient.getMedicalConditions() != null) {
            String conditions = patient.getMedicalConditions().toLowerCase();

            // Check contraindications
            if (medication.getContraindications() != null) {
                String contraindications = medication.getContraindications().toLowerCase();

                // Common condition checks
                if (conditions.contains("kidney") && contraindications.contains("renal")) {
                    warnings.add(Map.of(
                            "level", "WARNING",
                            "type", "CONDITION_CONTRAINDICATION",
                            "message", "⚠️ Caution: Patient has kidney disease",
                            "detail", "This medication may require dose adjustment in renal impairment",
                            "action", "Check creatinine clearance and adjust dose accordingly"
                    ));
                    hasWarning = true;
                }

                if (conditions.contains("liver") && contraindications.contains("hepatic")) {
                    warnings.add(Map.of(
                            "level", "WARNING",
                            "type", "CONDITION_CONTRAINDICATION",
                            "message", "⚠️ Caution: Patient has liver disease",
                            "detail", "This medication may require dose adjustment in hepatic impairment",
                            "action", "Check liver function and adjust dose accordingly"
                    ));
                    hasWarning = true;
                }

                if (conditions.contains("heart") && contraindications.contains("cardiac")) {
                    warnings.add(Map.of(
                            "level", "WARNING",
                            "type", "CONDITION_CONTRAINDICATION",
                            "message", "⚠️ Caution: Patient has heart condition",
                            "detail", "This medication may affect cardiac function",
                            "action", "Monitor closely and consider ECG"
                    ));
                    hasWarning = true;
                }
            }
        }

        // 5. CHECK PREGNANCY (for female patients of childbearing age)
        if (medication != null && "F".equals(patient.getSex()) && patientAge != null &&
                patientAge >= 12 && patientAge <= 55) {

            String pregCat = medication.getPregnancyCategory();
            if ("D".equals(pregCat) || "X".equals(pregCat)) {
                warnings.add(Map.of(
                        "level", "CRITICAL",
                        "type", "PREGNANCY_WARNING",
                        "message", "⚠️ PREGNANCY RISK CATEGORY " + pregCat,
                        "detail", "This medication can cause fetal harm",
                        "action", "Verify patient is not pregnant. Use contraception. Category X = absolutely contraindicated."
                ));
                hasCritical = true;
            } else if ("C".equals(pregCat)) {
                warnings.add(Map.of(
                        "level", "INFO",
                        "type", "PREGNANCY_WARNING",
                        "message", "ℹ️ Pregnancy Category C",
                        "detail", "Risk cannot be ruled out. Use only if benefits outweigh risks.",
                        "action", "Discuss with patient if pregnant or planning pregnancy"
                ));
            }
        }

        // 6. CHECK ALCOHOL WARNING
        if (medication != null && Boolean.TRUE.equals(medication.getAlcoholWarning())) {
            warnings.add(Map.of(
                    "level", "INFO",
                    "type", "LIFESTYLE",
                    "message", "ℹ️ Avoid Alcohol",
                    "detail", "This medication should not be taken with alcohol",
                    "action", "Instruct patient to avoid alcohol during treatment"
            ));
        }

        // 7. CHECK FOR DUPLICATE ACTIVE PRESCRIPTIONS
        if (medicationId != null) {
            List<Prescription> duplicates = prescriptionRepository.findActiveByPatientAndMedication(
                    patientId, medicationId, medicationName);

            if (!duplicates.isEmpty()) {
                warnings.add(Map.of(
                        "level", "WARNING",
                        "type", "DUPLICATE",
                        "message", "⚠️ Patient already has active prescription for this medication",
                        "detail", String.format("Active prescription: %s", duplicates.get(0).getPrescriptionNumber()),
                        "action", "Verify if new prescription is intended or if refill should be used instead"
                ));
                hasWarning = true;
            }
        }

        // Build result
        result.put("warnings", warnings);
        result.put("hasCritical", hasCritical);
        result.put("hasWarning", hasWarning);
        result.put("safeToPrescribe", !hasCritical);
        result.put("warningCount", warnings.size());

        // Generate summary message
        if (hasCritical) {
            result.put("summary", "CRITICAL WARNINGS DETECTED - Review carefully before prescribing");
        } else if (hasWarning) {
            result.put("summary", "Warnings detected - Use with caution");
        } else if (!warnings.isEmpty()) {
            result.put("summary", "Informational notices - Review with patient");
        } else {
            result.put("summary", "No interactions detected");
        }

        return result;
    }

    /**
     * Calculate suggested dosage based on patient factors
     */
    public Map<String, Object> calculateDosage(Long patientId, Long medicationId) {
        Map<String, Object> result = new HashMap<>();

        Patient patient = patientRepository.findById(patientId).orElse(null);
        Medication medication = medicationRepository.findById(medicationId).orElse(null);

        if (patient == null || medication == null) {
            result.put("error", "Patient or medication not found");
            return result;
        }

        // Calculate age
        Integer age = null;
        if (patient.getBirthDate() != null) {
            age = Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
        }

        // Start with default dosage
        String suggestedDosage = medication.getDefaultDosage();
        List<String> adjustmentNotes = new ArrayList<>();

        // Age-based adjustments
        if (age != null) {
            if (age < 12) {
                adjustmentNotes.add("Pediatric dosing may require adjustment based on weight");
                result.put("isPediatric", true);
            } else if (age > 65) {
                adjustmentNotes.add("Geriatric patient - consider starting with lower dose");
                result.put("isGeriatric", true);
            }
        }

        // Check for renal/hepatic adjustment needs
        if (Boolean.TRUE.equals(medication.getRenalAdjustment())) {
            adjustmentNotes.add("Requires renal function assessment for dose adjustment");
        }

        if (Boolean.TRUE.equals(medication.getHepaticAdjustment())) {
            adjustmentNotes.add("Requires liver function assessment for dose adjustment");
        }

        result.put("suggestedDosage", suggestedDosage);
        result.put("adjustmentNotes", adjustmentNotes);
        result.put("availableStrengths", medication.getStrengths());
        result.put("availableForms", medication.getDosageForms());
        result.put("availableRoutes", medication.getRoutes());

        return result;
    }

    /**
     * Update prescription status (auto-expire old prescriptions)
     */
    @Transactional
    public void updateExpiredPrescriptions() {
        List<Prescription> expired = prescriptionRepository.findExpired();

        for (Prescription rx : expired) {
            rx.setStatus("expired");
            prescriptionRepository.save(rx);

            auditService.log(
                    "PRESCRIPTION_AUTO_EXPIRED",
                    "PRESCRIPTION",
                    rx.getId().toString(),
                    null,
                    "system"
            );
        }
    }

    /**
     * Get prescriptions requiring follow-up
     */
    public List<Prescription> getFollowupDue(LocalDate upToDate) {
        return prescriptionRepository.findRequiringFollowup(upToDate);
    }

    /**
     * Calculate refill date prediction
     */
    public LocalDate predictRefillDate(Prescription prescription) {
        if (prescription.getQuantity() == null || prescription.getFrequencyCode() == null) {
            return null;
        }

        int dailyDoses = getDailyDosesFromFrequency(prescription.getFrequencyCode());
        if (dailyDoses == 0) return null;

        int daysSupply = prescription.getQuantity() / dailyDoses;
        return prescription.getStartDate().plusDays(daysSupply);
    }

    private int getDailyDosesFromFrequency(String frequencyCode) {
        if (frequencyCode == null) return 0;

        switch (frequencyCode.toUpperCase()) {
            case "QD": case "DAILY": return 1;
            case "BID": case "BD": return 2;
            case "TID": case "TD": return 3;
            case "QID": return 4;  // ✅ REMOVED DUPLICATE
            case "Q6H": return 4;
            case "Q8H": return 3;
            case "Q12H": return 2;
            default: return 1;
        }
    }
}