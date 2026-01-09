package com.team.medaibackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class StudyDetailDto {
    private Long id;
    private String studyUid;
    private PatientInfo patient;
    private LocalDateTime studyDate;
    private String description;
    private String modality;
    private String accessionNumber;
    private String status;
    private List<SeriesInfo> series;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static class PatientInfo {
        private Long id;
        private String patientId;
        private String name;
        private String sex;
        private String birthDate;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSex() { return sex; }
        public void setSex(String sex) { this.sex = sex; }

        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    }

    public static class SeriesInfo {
        private Long id;
        private String seriesUid;
        private Integer seriesNumber;
        private String modality;
        private String description;
        private String manufacturer;
        private Integer imageCount;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getSeriesUid() { return seriesUid; }
        public void setSeriesUid(String seriesUid) { this.seriesUid = seriesUid; }

        public Integer getSeriesNumber() { return seriesNumber; }
        public void setSeriesNumber(Integer seriesNumber) { this.seriesNumber = seriesNumber; }

        public String getModality() { return modality; }
        public void setModality(String modality) { this.modality = modality; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

        public Integer getImageCount() { return imageCount; }
        public void setImageCount(Integer imageCount) { this.imageCount = imageCount; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudyUid() { return studyUid; }
    public void setStudyUid(String studyUid) { this.studyUid = studyUid; }

    public PatientInfo getPatient() { return patient; }
    public void setPatient(PatientInfo patient) { this.patient = patient; }

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

    public List<SeriesInfo> getSeries() { return series; }
    public void setSeries(List<SeriesInfo> series) { this.series = series; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}