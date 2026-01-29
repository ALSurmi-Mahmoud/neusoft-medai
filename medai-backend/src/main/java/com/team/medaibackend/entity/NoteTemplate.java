package com.team.medaibackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "note_templates")
public class NoteTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "note_type", nullable = false, length = 50)
    private String noteType; // soap, progress, consultation, procedure, discharge, followup

    @Column(columnDefinition = "TEXT")
    private String description;

    // Template Content for SOAP notes
    @Column(name = "subjective_template", columnDefinition = "TEXT")
    private String subjectiveTemplate;

    @Column(name = "objective_template", columnDefinition = "TEXT")
    private String objectiveTemplate;

    @Column(name = "assessment_template", columnDefinition = "TEXT")
    private String assessmentTemplate;

    @Column(name = "plan_template", columnDefinition = "TEXT")
    private String planTemplate;

    // Template Content for general notes
    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;

    // Template Metadata
    @Column(name = "is_system")
    private Boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(length = 100)
    private String specialty; // cardiology, orthopedics, general, etc.

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public String getNoteType() { return noteType; }
    public void setNoteType(String noteType) { this.noteType = noteType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubjectiveTemplate() { return subjectiveTemplate; }
    public void setSubjectiveTemplate(String subjectiveTemplate) { this.subjectiveTemplate = subjectiveTemplate; }

    public String getObjectiveTemplate() { return objectiveTemplate; }
    public void setObjectiveTemplate(String objectiveTemplate) { this.objectiveTemplate = objectiveTemplate; }

    public String getAssessmentTemplate() { return assessmentTemplate; }
    public void setAssessmentTemplate(String assessmentTemplate) { this.assessmentTemplate = assessmentTemplate; }

    public String getPlanTemplate() { return planTemplate; }
    public void setPlanTemplate(String planTemplate) { this.planTemplate = planTemplate; }

    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}