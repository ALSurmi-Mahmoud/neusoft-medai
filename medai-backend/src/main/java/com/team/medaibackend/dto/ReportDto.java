package com.team.medaibackend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ReportDto {
    private Long id;
    private String reportUid;
    private Long studyId;
    private String studyUid;
    private String patientId;
    private Long authorId;
    private String authorName;
    private String title;
    private String summary;
    private String findings;
    private String impression;
    private String recommendations;
    private Map<String, Object> content;
    private String status;
    private Boolean finalized;
    private LocalDateTime finalizedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String patientName;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportUid() { return reportUid; }
    public void setReportUid(String reportUid) { this.reportUid = reportUid; }

    public Long getStudyId() { return studyId; }
    public void setStudyId(Long studyId) { this.studyId = studyId; }

    public String getStudyUid() { return studyUid; }
    public void setStudyUid(String studyUid) { this.studyUid = studyUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public String getImpression() { return impression; }
    public void setImpression(String impression) { this.impression = impression; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getFinalized() { return finalized; }
    public void setFinalized(Boolean finalized) { this.finalized = finalized; }

    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime finalizedAt) { this.finalizedAt = finalizedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
}