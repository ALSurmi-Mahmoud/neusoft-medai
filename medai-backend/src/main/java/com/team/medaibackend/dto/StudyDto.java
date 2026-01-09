package com.team.medaibackend.dto;

import java.time.LocalDateTime;

public class StudyDto {
    private Long id;
    private String studyUid;
    private String patientId;
    private String patientName;
    private LocalDateTime studyDate;
    private String description;
    private String modality;
    private String accessionNumber;
    private String status;
    private Integer seriesCount;
    private Integer instanceCount;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudyUid() { return studyUid; }
    public void setStudyUid(String studyUid) { this.studyUid = studyUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public LocalDateTime getStudyDate() { return studyDate; }
    public void setStudyDate(LocalDateTime studyDate) { this.studyDate = studyDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSeriesCount() { return seriesCount; }
    public void setSeriesCount(Integer seriesCount) { this.seriesCount = seriesCount; }

    public Integer getInstanceCount() { return instanceCount; }
    public void setInstanceCount(Integer instanceCount) { this.instanceCount = instanceCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}