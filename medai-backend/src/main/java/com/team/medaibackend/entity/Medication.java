package com.team.medaibackend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Info
    @Column(nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "drug_class", length = 100)
    private String drugClass;

    // Identification
    @Column(name = "ndc_code", length = 50)
    private String ndcCode;

    @Column(length = 50)
    private String rxcui;

    // Drug Information
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String indications;

    @Column(columnDefinition = "TEXT")
    private String contraindications;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    // Dosage Information
    @Column(name = "default_dosage", length = 100)
    private String defaultDosage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dosage_forms", columnDefinition = "text")
    private List<String> dosageForms;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private List<String> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private List<String> routes;

    // Special Considerations
    @Column(name = "requires_monitoring")
    private Boolean requiresMonitoring = false;

    @Column(name = "controlled_substance")
    private Boolean controlledSubstance = false;

    @Column(name = "schedule_class", length = 10)
    private String scheduleClass;

    // Interaction Data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interaction_warnings", columnDefinition = "text")
    private List<String> interactionWarnings;

    @Column(name = "food_interactions", columnDefinition = "TEXT")
    private String foodInteractions;

    @Column(name = "alcohol_warning")
    private Boolean alcoholWarning = false;

    @Column(name = "pregnancy_category", length = 5)
    private String pregnancyCategory;

    // Age/Condition Restrictions
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "renal_adjustment")
    private Boolean renalAdjustment = false;

    @Column(name = "hepatic_adjustment")
    private Boolean hepaticAdjustment = false;

    // Search/Display
    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column
    private Boolean active = true;

    // Audit
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getDrugClass() { return drugClass; }
    public void setDrugClass(String drugClass) { this.drugClass = drugClass; }

    public String getNdcCode() { return ndcCode; }
    public void setNdcCode(String ndcCode) { this.ndcCode = ndcCode; }

    public String getRxcui() { return rxcui; }
    public void setRxcui(String rxcui) { this.rxcui = rxcui; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIndications() { return indications; }
    public void setIndications(String indications) { this.indications = indications; }

    public String getContraindications() { return contraindications; }
    public void setContraindications(String contraindications) { this.contraindications = contraindications; }

    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }

    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }

    public String getDefaultDosage() { return defaultDosage; }
    public void setDefaultDosage(String defaultDosage) { this.defaultDosage = defaultDosage; }

    public List<String> getDosageForms() { return dosageForms; }
    public void setDosageForms(List<String> dosageForms) { this.dosageForms = dosageForms; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getRoutes() { return routes; }
    public void setRoutes(List<String> routes) { this.routes = routes; }

    public Boolean getRequiresMonitoring() { return requiresMonitoring; }
    public void setRequiresMonitoring(Boolean requiresMonitoring) { this.requiresMonitoring = requiresMonitoring; }

    public Boolean getControlledSubstance() { return controlledSubstance; }
    public void setControlledSubstance(Boolean controlledSubstance) { this.controlledSubstance = controlledSubstance; }

    public String getScheduleClass() { return scheduleClass; }
    public void setScheduleClass(String scheduleClass) { this.scheduleClass = scheduleClass; }

    public List<String> getInteractionWarnings() { return interactionWarnings; }
    public void setInteractionWarnings(List<String> interactionWarnings) { this.interactionWarnings = interactionWarnings; }

    public String getFoodInteractions() { return foodInteractions; }
    public void setFoodInteractions(String foodInteractions) { this.foodInteractions = foodInteractions; }

    public Boolean getAlcoholWarning() { return alcoholWarning; }
    public void setAlcoholWarning(Boolean alcoholWarning) { this.alcoholWarning = alcoholWarning; }

    public String getPregnancyCategory() { return pregnancyCategory; }
    public void setPregnancyCategory(String pregnancyCategory) { this.pregnancyCategory = pregnancyCategory; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public Boolean getRenalAdjustment() { return renalAdjustment; }
    public void setRenalAdjustment(Boolean renalAdjustment) { this.renalAdjustment = renalAdjustment; }

    public Boolean getHepaticAdjustment() { return hepaticAdjustment; }
    public void setHepaticAdjustment(Boolean hepaticAdjustment) { this.hepaticAdjustment = hepaticAdjustment; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}