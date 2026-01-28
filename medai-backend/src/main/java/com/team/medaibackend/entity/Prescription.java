package com.team.medaibackend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identification
    @Column(name = "prescription_number", unique = true, nullable = false, length = 50)
    private String prescriptionNumber;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id")
    private Medication medication;

    // Manual medication entry (if not in medications table)
    @Column(name = "medication_name")
    private String medicationName;

    // Prescription Details
    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(name = "dosage_form", nullable = false, length = 50)
    private String dosageForm;

    @Column(nullable = false, length = 50)
    private String route;

    @Column(nullable = false, length = 100)
    private String frequency;

    @Column(name = "frequency_code", length = 20)
    private String frequencyCode;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(nullable = false)
    private Integer quantity;

    // Refills
    @Column(name = "refills_allowed")
    private Integer refillsAllowed = 0;

    @Column(name = "refills_remaining")
    private Integer refillsRemaining = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "refill_history", columnDefinition = "jsonb")
    private Map<String, Object> refillHistory;

    // Instructions
    @Column(nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "patient_instructions", columnDefinition = "TEXT")
    private String patientInstructions;

    // Timing
    @Column(name = "timing_morning")
    private Boolean timingMorning = false;

    @Column(name = "timing_afternoon")
    private Boolean timingAfternoon = false;

    @Column(name = "timing_evening")
    private Boolean timingEvening = false;

    @Column(name = "timing_bedtime")
    private Boolean timingBedtime = false;

    @Column(name = "timing_as_needed")
    private Boolean timingAsNeeded = false;

    // Status and Dates
    @Column(length = 50)
    private String status = "active";

    @Column(name = "prescribed_date", nullable = false)
    private LocalDate prescribedDate = LocalDate.now();

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "last_refill_date")
    private LocalDate lastRefillDate;

    // Validation and Security
    @Column
    private Boolean verified = false;

    @Column(name = "verification_code", length = 10)
    private String verificationCode;

    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature;

    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    // Drug Safety Checks
    @Column(name = "interaction_check_passed")
    private Boolean interactionCheckPassed = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interaction_warnings", columnDefinition = "jsonb")
    private Map<String, Object> interactionWarnings;

    @Column(name = "allergy_override")
    private Boolean allergyOverride = false;

    @Column(name = "allergy_override_reason", columnDefinition = "TEXT")
    private String allergyOverrideReason;

    // Pharmacy Information
    @Column(name = "pharmacy_name")
    private String pharmacyName;

    @Column(name = "pharmacy_phone", length = 20)
    private String pharmacyPhone;

    @Column(name = "pharmacy_fax", length = 20)
    private String pharmacyFax;

    @Column
    private Boolean dispensed = false;

    @Column(name = "dispensed_date")
    private LocalDate dispensedDate;

    @Column(name = "dispensed_by")
    private String dispensedBy;

    // Follow-up
    @Column(name = "requires_followup")
    private Boolean requiresFollowup = false;

    @Column(name = "followup_date")
    private LocalDate followupDate;

    @Column(name = "followup_notes", columnDefinition = "TEXT")
    private String followupNotes;

    // Audit
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (prescribedDate == null) {
            prescribedDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrescriptionNumber() { return prescriptionNumber; }
    public void setPrescriptionNumber(String prescriptionNumber) { this.prescriptionNumber = prescriptionNumber; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    public Medication getMedication() { return medication; }
    public void setMedication(Medication medication) { this.medication = medication; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getFrequencyCode() { return frequencyCode; }
    public void setFrequencyCode(String frequencyCode) { this.frequencyCode = frequencyCode; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getRefillsAllowed() { return refillsAllowed; }
    public void setRefillsAllowed(Integer refillsAllowed) { this.refillsAllowed = refillsAllowed; }

    public Integer getRefillsRemaining() { return refillsRemaining; }
    public void setRefillsRemaining(Integer refillsRemaining) { this.refillsRemaining = refillsRemaining; }

    public Map<String, Object> getRefillHistory() { return refillHistory; }
    public void setRefillHistory(Map<String, Object> refillHistory) { this.refillHistory = refillHistory; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPatientInstructions() { return patientInstructions; }
    public void setPatientInstructions(String patientInstructions) { this.patientInstructions = patientInstructions; }

    public Boolean getTimingMorning() { return timingMorning; }
    public void setTimingMorning(Boolean timingMorning) { this.timingMorning = timingMorning; }

    public Boolean getTimingAfternoon() { return timingAfternoon; }
    public void setTimingAfternoon(Boolean timingAfternoon) { this.timingAfternoon = timingAfternoon; }

    public Boolean getTimingEvening() { return timingEvening; }
    public void setTimingEvening(Boolean timingEvening) { this.timingEvening = timingEvening; }

    public Boolean getTimingBedtime() { return timingBedtime; }
    public void setTimingBedtime(Boolean timingBedtime) { this.timingBedtime = timingBedtime; }

    public Boolean getTimingAsNeeded() { return timingAsNeeded; }
    public void setTimingAsNeeded(Boolean timingAsNeeded) { this.timingAsNeeded = timingAsNeeded; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getPrescribedDate() { return prescribedDate; }
    public void setPrescribedDate(LocalDate prescribedDate) { this.prescribedDate = prescribedDate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getLastRefillDate() { return lastRefillDate; }
    public void setLastRefillDate(LocalDate lastRefillDate) { this.lastRefillDate = lastRefillDate; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public Boolean getInteractionCheckPassed() { return interactionCheckPassed; }
    public void setInteractionCheckPassed(Boolean interactionCheckPassed) { this.interactionCheckPassed = interactionCheckPassed; }

    public Map<String, Object> getInteractionWarnings() { return interactionWarnings; }
    public void setInteractionWarnings(Map<String, Object> interactionWarnings) { this.interactionWarnings = interactionWarnings; }

    public Boolean getAllergyOverride() { return allergyOverride; }
    public void setAllergyOverride(Boolean allergyOverride) { this.allergyOverride = allergyOverride; }

    public String getAllergyOverrideReason() { return allergyOverrideReason; }
    public void setAllergyOverrideReason(String allergyOverrideReason) { this.allergyOverrideReason = allergyOverrideReason; }

    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }

    public String getPharmacyPhone() { return pharmacyPhone; }
    public void setPharmacyPhone(String pharmacyPhone) { this.pharmacyPhone = pharmacyPhone; }

    public String getPharmacyFax() { return pharmacyFax; }
    public void setPharmacyFax(String pharmacyFax) { this.pharmacyFax = pharmacyFax; }

    public Boolean getDispensed() { return dispensed; }
    public void setDispensed(Boolean dispensed) { this.dispensed = dispensed; }

    public LocalDate getDispensedDate() { return dispensedDate; }
    public void setDispensedDate(LocalDate dispensedDate) { this.dispensedDate = dispensedDate; }

    public String getDispensedBy() { return dispensedBy; }
    public void setDispensedBy(String dispensedBy) { this.dispensedBy = dispensedBy; }

    public Boolean getRequiresFollowup() { return requiresFollowup; }
    public void setRequiresFollowup(Boolean requiresFollowup) { this.requiresFollowup = requiresFollowup; }

    public LocalDate getFollowupDate() { return followupDate; }
    public void setFollowupDate(LocalDate followupDate) { this.followupDate = followupDate; }

    public String getFollowupNotes() { return followupNotes; }
    public void setFollowupNotes(String followupNotes) { this.followupNotes = followupNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Long getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Long cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}