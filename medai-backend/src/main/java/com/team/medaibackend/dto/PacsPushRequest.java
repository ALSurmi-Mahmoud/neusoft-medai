package com.team.medaibackend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for pushing a study to PACS.
 */
public class PacsPushRequest {

    @NotBlank(message = "Study ID is required")
    private String studyId;

    private String targetModality;
    private Boolean includeReports;

    // Getters and Setters
    public String getStudyId() { return studyId; }
    public void setStudyId(String studyId) { this.studyId = studyId; }

    public String getTargetModality() { return targetModality; }
    public void setTargetModality(String targetModality) { this.targetModality = targetModality; }

    public Boolean getIncludeReports() { return includeReports; }
    public void setIncludeReports(Boolean includeReports) { this.includeReports = includeReports; }
}