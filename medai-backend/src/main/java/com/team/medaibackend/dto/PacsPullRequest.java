package com.team.medaibackend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for pulling a study from PACS.
 */
public class PacsPullRequest {

    @NotBlank(message = "Study Instance UID is required")
    private String studyInstanceUid;

    private String patientId;
    private String accessionNumber;

    // Getters and Setters
    public String getStudyInstanceUid() { return studyInstanceUid; }
    public void setStudyInstanceUid(String studyInstanceUid) { this.studyInstanceUid = studyInstanceUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }
}