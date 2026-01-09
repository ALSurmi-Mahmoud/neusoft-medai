package com.team.medaibackend.web;

import com.team.medaibackend.dicom.DicomMetadata;
import com.team.medaibackend.dicom.DicomParser;
import com.team.medaibackend.dicom.DicomTags;
import com.team.medaibackend.storage.FileStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dicom")
public class DicomTestController {

    private final DicomParser dicomParser;
    private final FileStorage fileStorage;

    public DicomTestController(DicomParser dicomParser, FileStorage fileStorage) {
        this.dicomParser = dicomParser;
        this.fileStorage = fileStorage;
    }

    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseDicom(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Store the uploaded file temporarily
            String storedPath = fileStorage.store(file, "temp");
            Path fullPath = fileStorage.getFullPath(storedPath);

            // Parse the file
            DicomMetadata metadata = dicomParser.parse(fullPath);

            result.put("success", true);
            result.put("storedPath", storedPath);
            result.put("metadata", metadataToMap(metadata));

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/parse-existing")
    public ResponseEntity<Map<String, Object>> parseExisting(@RequestParam String path) {
        Map<String, Object> result = new HashMap<>();

        try {
            Path fullPath = fileStorage.getFullPath(path);

            if (!fileStorage.exists(path)) {
                result.put("success", false);
                result.put("error", "File not found: " + path);
                return ResponseEntity.notFound().build();
            }

            DicomMetadata metadata = dicomParser.parse(fullPath);

            result.put("success", true);
            result.put("path", path);
            result.put("metadata", metadataToMap(metadata));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/tags")
    public ResponseEntity<Map<String, Object>> getDicomTags() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> tags = new HashMap<>();

        // Return some common DICOM tag references
        tags.put("PatientID", DicomTags.tagToString(DicomTags.PATIENT_ID));
        tags.put("PatientName", DicomTags.tagToString(DicomTags.PATIENT_NAME));
        tags.put("StudyInstanceUID", DicomTags.tagToString(DicomTags.STUDY_INSTANCE_UID));
        tags.put("SeriesInstanceUID", DicomTags.tagToString(DicomTags.SERIES_INSTANCE_UID));
        tags.put("SOPInstanceUID", DicomTags.tagToString(DicomTags.SOP_INSTANCE_UID));
        tags.put("Modality", DicomTags.tagToString(DicomTags.MODALITY));
        tags.put("Rows", DicomTags.tagToString(DicomTags.ROWS));
        tags.put("Columns", DicomTags.tagToString(DicomTags.COLUMNS));
        tags.put("PixelSpacing", DicomTags.tagToString(DicomTags.PIXEL_SPACING));
        tags.put("SliceThickness", DicomTags.tagToString(DicomTags.SLICE_THICKNESS));

        result.put("commonTags", tags);
        result.put("note", "These are standard DICOM tag references in (GGGG,EEEE) format");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-synthetic")
    public ResponseEntity<Map<String, Object>> testSyntheticParse() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Create a dummy test file
            String testContent = "TEST DICOM PLACEHOLDER - NOT A REAL DICOM FILE";
            String filename = "test-" + System.currentTimeMillis() + ".dcm";
            String storedPath = fileStorage.store(testContent.getBytes(), filename, "temp");

            // Parse it (will get synthetic metadata since it's not real DICOM)
            Path fullPath = fileStorage.getFullPath(storedPath);
            DicomMetadata metadata = dicomParser.parse(fullPath);

            result.put("success", true);
            result.put("note", "This is synthetic metadata from stub parser (file is not real DICOM)");
            result.put("storedPath", storedPath);
            result.put("isDicomFile", metadata.isParsedSuccessfully());
            result.put("metadata", metadataToMap(metadata));

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    private Map<String, Object> metadataToMap(DicomMetadata metadata) {
        Map<String, Object> map = new HashMap<>();

        // Patient Info
        Map<String, Object> patient = new HashMap<>();
        patient.put("patientId", metadata.getPatientId());
        patient.put("patientName", metadata.getPatientName());
        patient.put("patientSex", metadata.getPatientSex());
        patient.put("patientBirthDate", metadata.getPatientBirthDate() != null ? metadata.getPatientBirthDate().toString() : null);
        map.put("patient", patient);

        // Study Info
        Map<String, Object> study = new HashMap<>();
        study.put("studyInstanceUid", metadata.getStudyInstanceUid());
        study.put("studyDateTime", metadata.getStudyDateTime() != null ? metadata.getStudyDateTime().toString() : null);
        study.put("studyDescription", metadata.getStudyDescription());
        study.put("accessionNumber", metadata.getAccessionNumber());
        map.put("study", study);

        // Series Info
        Map<String, Object> series = new HashMap<>();
        series.put("seriesInstanceUid", metadata.getSeriesInstanceUid());
        series.put("seriesNumber", metadata.getSeriesNumber());
        series.put("modality", metadata.getModality());
        series.put("bodyPartExamined", metadata.getBodyPartExamined());
        map.put("series", series);

        // Instance Info
        Map<String, Object> instance = new HashMap<>();
        instance.put("sopInstanceUid", metadata.getSopInstanceUid());
        instance.put("instanceNumber", metadata.getInstanceNumber());
        map.put("instance", instance);

        // Image Info
        Map<String, Object> image = new HashMap<>();
        image.put("rows", metadata.getRows());
        image.put("columns", metadata.getColumns());
        image.put("bitsAllocated", metadata.getBitsAllocated());
        image.put("pixelSpacing", metadata.getPixelSpacing());
        image.put("sliceThickness", metadata.getSliceThickness());
        image.put("windowCenter", metadata.getWindowCenter());
        image.put("windowWidth", metadata.getWindowWidth());
        map.put("image", image);

        // Equipment Info
        Map<String, Object> equipment = new HashMap<>();
        equipment.put("manufacturer", metadata.getManufacturer());
        equipment.put("institutionName", metadata.getInstitutionName());
        map.put("equipment", equipment);

        // File Info
        Map<String, Object> file = new HashMap<>();
        file.put("filePath", metadata.getFilePath());
        file.put("fileSize", metadata.getFileSize());
        map.put("file", file);

        // Parse Status
        map.put("parsedSuccessfully", metadata.isParsedSuccessfully());
        map.put("parseError", metadata.getParseError());
        map.put("additionalTags", metadata.getAdditionalTags());

        return map;
    }
}