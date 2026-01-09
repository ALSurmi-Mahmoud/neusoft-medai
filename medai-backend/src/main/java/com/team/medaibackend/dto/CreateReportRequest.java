package com.team.medaibackend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class CreateReportRequest {

    @NotNull(message = "Study ID is required")
    private Long studyId;

    private String title;
    private String summary;
    private String findings;
    private String impression;
    private String recommendations;
    private Map<String, Object> content;

    // Getters and Setters
    public Long getStudyId() { return studyId; }
    public void setStudyId(Long studyId) { this.studyId = studyId; }

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
}