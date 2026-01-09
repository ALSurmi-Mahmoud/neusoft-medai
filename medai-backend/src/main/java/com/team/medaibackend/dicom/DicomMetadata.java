package com.team.medaibackend.dicom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object for DICOM file metadata.
 * Contains parsed information from DICOM headers.
 */
public class DicomMetadata {

    // Patient Information
    private String patientId;
    private String patientName;
    private String patientSex;
    private LocalDate patientBirthDate;
    private String patientAge;

    // Study Information
    private String studyInstanceUid;
    private LocalDateTime studyDateTime;
    private String studyDescription;
    private String studyId;
    private String accessionNumber;

    // Series Information
    private String seriesInstanceUid;
    private Integer seriesNumber;
    private String seriesDescription;
    private String modality;
    private String bodyPartExamined;

    // Instance Information
    private String sopInstanceUid;
    private String sopClassUid;
    private Integer instanceNumber;

    // Image Information
    private Integer rows;
    private Integer columns;
    private Integer bitsAllocated;
    private Integer bitsStored;
    private String photometricInterpretation;
    private String pixelSpacing;
    private Double sliceThickness;
    private Double sliceLocation;
    private String imagePositionPatient;
    private String imageOrientationPatient;
    private String windowCenter;
    private String windowWidth;

    // Equipment Information
    private String manufacturer;
    private String institutionName;
    private String stationName;
    private String manufacturerModelName;

    // Transfer Syntax
    private String transferSyntaxUid;

    // File Information (not from DICOM header)
    private String filePath;
    private Long fileSize;

    // Additional/custom tags
    private Map<String, Object> additionalTags = new HashMap<>();

    // Parsing status
    private boolean parsedSuccessfully;
    private String parseError;

    // ========================================
    // Getters and Setters
    // ========================================

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientSex() { return patientSex; }
    public void setPatientSex(String patientSex) { this.patientSex = patientSex; }

    public LocalDate getPatientBirthDate() { return patientBirthDate; }
    public void setPatientBirthDate(LocalDate patientBirthDate) { this.patientBirthDate = patientBirthDate; }

    public String getPatientAge() { return patientAge; }
    public void setPatientAge(String patientAge) { this.patientAge = patientAge; }

    public String getStudyInstanceUid() { return studyInstanceUid; }
    public void setStudyInstanceUid(String studyInstanceUid) { this.studyInstanceUid = studyInstanceUid; }

    public LocalDateTime getStudyDateTime() { return studyDateTime; }
    public void setStudyDateTime(LocalDateTime studyDateTime) { this.studyDateTime = studyDateTime; }

    public String getStudyDescription() { return studyDescription; }
    public void setStudyDescription(String studyDescription) { this.studyDescription = studyDescription; }

    public String getStudyId() { return studyId; }
    public void setStudyId(String studyId) { this.studyId = studyId; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getSeriesInstanceUid() { return seriesInstanceUid; }
    public void setSeriesInstanceUid(String seriesInstanceUid) { this.seriesInstanceUid = seriesInstanceUid; }

    public Integer getSeriesNumber() { return seriesNumber; }
    public void setSeriesNumber(Integer seriesNumber) { this.seriesNumber = seriesNumber; }

    public String getSeriesDescription() { return seriesDescription; }
    public void setSeriesDescription(String seriesDescription) { this.seriesDescription = seriesDescription; }

    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }

    public String getBodyPartExamined() { return bodyPartExamined; }
    public void setBodyPartExamined(String bodyPartExamined) { this.bodyPartExamined = bodyPartExamined; }

    public String getSopInstanceUid() { return sopInstanceUid; }
    public void setSopInstanceUid(String sopInstanceUid) { this.sopInstanceUid = sopInstanceUid; }

    public String getSopClassUid() { return sopClassUid; }
    public void setSopClassUid(String sopClassUid) { this.sopClassUid = sopClassUid; }

    public Integer getInstanceNumber() { return instanceNumber; }
    public void setInstanceNumber(Integer instanceNumber) { this.instanceNumber = instanceNumber; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getColumns() { return columns; }
    public void setColumns(Integer columns) { this.columns = columns; }

    public Integer getBitsAllocated() { return bitsAllocated; }
    public void setBitsAllocated(Integer bitsAllocated) { this.bitsAllocated = bitsAllocated; }

    public Integer getBitsStored() { return bitsStored; }
    public void setBitsStored(Integer bitsStored) { this.bitsStored = bitsStored; }

    public String getPhotometricInterpretation() { return photometricInterpretation; }
    public void setPhotometricInterpretation(String photometricInterpretation) { this.photometricInterpretation = photometricInterpretation; }

    public String getPixelSpacing() { return pixelSpacing; }
    public void setPixelSpacing(String pixelSpacing) { this.pixelSpacing = pixelSpacing; }

    public Double getSliceThickness() { return sliceThickness; }
    public void setSliceThickness(Double sliceThickness) { this.sliceThickness = sliceThickness; }

    public Double getSliceLocation() { return sliceLocation; }
    public void setSliceLocation(Double sliceLocation) { this.sliceLocation = sliceLocation; }

    public String getImagePositionPatient() { return imagePositionPatient; }
    public void setImagePositionPatient(String imagePositionPatient) { this.imagePositionPatient = imagePositionPatient; }

    public String getImageOrientationPatient() { return imageOrientationPatient; }
    public void setImageOrientationPatient(String imageOrientationPatient) { this.imageOrientationPatient = imageOrientationPatient; }

    public String getWindowCenter() { return windowCenter; }
    public void setWindowCenter(String windowCenter) { this.windowCenter = windowCenter; }

    public String getWindowWidth() { return windowWidth; }
    public void setWindowWidth(String windowWidth) { this.windowWidth = windowWidth; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getManufacturerModelName() { return manufacturerModelName; }
    public void setManufacturerModelName(String manufacturerModelName) { this.manufacturerModelName = manufacturerModelName; }

    public String getTransferSyntaxUid() { return transferSyntaxUid; }
    public void setTransferSyntaxUid(String transferSyntaxUid) { this.transferSyntaxUid = transferSyntaxUid; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Map<String, Object> getAdditionalTags() { return additionalTags; }
    public void setAdditionalTags(Map<String, Object> additionalTags) { this.additionalTags = additionalTags; }

    public void addTag(String tagName, Object value) {
        this.additionalTags.put(tagName, value);
    }

    public boolean isParsedSuccessfully() { return parsedSuccessfully; }
    public void setParsedSuccessfully(boolean parsedSuccessfully) { this.parsedSuccessfully = parsedSuccessfully; }

    public String getParseError() { return parseError; }
    public void setParseError(String parseError) { this.parseError = parseError; }

    @Override
    public String toString() {
        return "DicomMetadata{" +
                "patientId='" + patientId + '\'' +
                ", studyInstanceUid='" + studyInstanceUid + '\'' +
                ", seriesInstanceUid='" + seriesInstanceUid + '\'' +
                ", sopInstanceUid='" + sopInstanceUid + '\'' +
                ", modality='" + modality + '\'' +
                ", rows=" + rows +
                ", columns=" + columns +
                '}';
    }
}