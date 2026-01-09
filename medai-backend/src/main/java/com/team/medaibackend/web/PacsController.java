package com.team.medaibackend.web;

import com.team.medaibackend.dto.PacsPullRequest;
import com.team.medaibackend.dto.PacsPushRequest;
import com.team.medaibackend.pacs.OrthancClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for PACS operations.
 *
 * Provides endpoints for:
 * - Pulling studies from Orthanc PACS
 * - Pushing studies to Orthanc PACS
 * - Querying available studies
 * - Checking PACS connection status
 */
@RestController
@RequestMapping("/api/pacs")
public class PacsController {

    private final OrthancClient orthancClient;

    public PacsController(OrthancClient orthancClient) {
        this.orthancClient = orthancClient;
    }

    /**
     * Get PACS/Orthanc connection status and configuration.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("configuration", orthancClient.getConfiguration());
        status.put("systemInfo", orthancClient.getSystemInfo());
        status.put("available", orthancClient.isAvailable());

        return ResponseEntity.ok(status);
    }

    /**
     * Pull a study from PACS by Study Instance UID.
     *
     * Example request:
     * POST /api/pacs/pull
     * {
     *   "studyInstanceUid": "1.2.840.113619.2.55.3.604688119.969.1364202859.74"
     * }
     */
    @PostMapping("/pull")
    public ResponseEntity<Map<String, Object>> pullStudy(@Valid @RequestBody PacsPullRequest request) {
        Map<String, Object> response = new HashMap<>();

        OrthancClient.PullResult result = orthancClient.pullStudy(request.getStudyInstanceUid());

        response.put("taskId", result.getTaskId());
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        response.put("studyInstanceUid", result.getStudyInstanceUid());
        response.put("requestedAt", result.getRequestedAt());

        if (result.getOrthancStudyId() != null) {
            response.put("orthancStudyId", result.getOrthancStudyId());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Push a study to PACS.
     *
     * Example request:
     * POST /api/pacs/push
     * {
     *   "studyId": "123",
     *   "targetModality": "REMOTE_PACS"
     * }
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> pushStudy(@Valid @RequestBody PacsPushRequest request) {
        Map<String, Object> response = new HashMap<>();

        OrthancClient.PushResult result = orthancClient.pushStudy(request.getStudyId());

        response.put("taskId", result.getTaskId());
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        response.put("studyId", result.getStudyId());
        response.put("requestedAt", result.getRequestedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Query PACS for available studies.
     */
    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryStudies(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) String studyDate) {

        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> studies = orthancClient.queryStudies(patientId, modality, studyDate);

        response.put("query", Map.of(
                "patientId", patientId != null ? patientId : "",
                "modality", modality != null ? modality : "",
                "studyDate", studyDate != null ? studyDate : ""
        ));
        response.put("results", studies);
        response.put("count", studies.size());
        response.put("note", "STUB: Real implementation would query Orthanc PACS");

        return ResponseEntity.ok(response);
    }

    /**
     * Get information about TCIA C4KC-KiTS demo dataset.
     * This provides documentation for the recommended test dataset.
     */
    @GetMapping("/demo-dataset-info")
    public ResponseEntity<Map<String, Object>> getDemoDatasetInfo() {
        Map<String, Object> info = new HashMap<>();

        // Dataset description
        info.put("name", "TCIA C4KC-KiTS (Kidney Tumor Segmentation Challenge)");
        info.put("description",
                "The C4KC-KiTS dataset is a publicly available, de-identified collection of " +
                        "contrast-enhanced CT scans from patients with kidney tumors. It was created for " +
                        "the KiTS (Kidney Tumor Segmentation) Challenge and contains high-quality DICOM " +
                        "images with expert segmentation annotations for kidneys and tumors.");

        // Why this dataset
        info.put("selectionRationale", Map.of(
                "realistic", "Contains real clinical CT scans with proper DICOM metadata structure",
                "deIdentified", "All patient information has been removed, safe for research/demo use",
                "accessible", "Freely downloadable from The Cancer Imaging Archive (TCIA)",
                "wellDocumented", "Widely used in research with extensive documentation",
                "relevantModality", "CT imaging is common in medical diagnosis workflows"
        ));

        // Download instructions
        info.put("downloadInstructions", Map.of(
                "step1", "Visit TCIA website: https://www.cancerimagingarchive.net/",
                "step2", "Search for 'C4KC-KiTS' or 'KiTS19' dataset",
                "step3", "Use NBIA Data Retriever to download DICOM files",
                "step4", "Select a subset of cases for demo (e.g., case_00000 to case_00009)",
                "alternativeDownload", "Direct link: https://wiki.cancerimagingarchive.net/display/Public/C4KC-KiTS"
        ));

        // Orthanc import instructions
        info.put("orthancImportInstructions", Map.of(
                "method1_webInterface", Map.of(
                        "description", "Upload via Orthanc Explorer web interface",
                        "steps", List.of(
                                "1. Open Orthanc Explorer at http://localhost:8042",
                                "2. Click 'Upload' in the top menu",
                                "3. Drag and drop DICOM files or select folder",
                                "4. Wait for upload to complete"
                        )
                ),
                "method2_restApi", Map.of(
                        "description", "Upload via REST API using curl",
                        "command", "curl -X POST http://localhost:8042/instances -u orthanc:orthanc --data-binary @/path/to/file.dcm",
                        "batchScript", "for f in /path/to/dicom/*.dcm; do curl -X POST http://localhost:8042/instances -u orthanc:orthanc --data-binary @$f; done"
                ),
                "method3_filesystem", Map.of(
                        "description", "Copy files to Orthanc storage directory",
                        "note", "Orthanc can auto-import from configured directories"
                )
        ));

        // Local storage simulation
        info.put("localStorageSimulation", Map.of(
                "description", "For testing without Orthanc, place DICOM files in the storage directory",
                "steps", List.of(
                        "1. Create directory: storage/dicom/demo/",
                        "2. Copy sample DICOM files to this directory",
                        "3. Use /api/dicom/parse endpoint to parse individual files",
                        "4. System will generate synthetic metadata for testing"
                ),
                "command", "cp /path/to/downloaded/case_00000/*.dcm storage/dicom/demo/"
        ));

        // Sample Study UIDs for testing
        info.put("sampleStudyUids", List.of(
                "1.2.840.113619.2.55.3.604688119.969.1364202859.74",
                "1.2.840.113619.2.55.3.604688119.969.1364202859.75",
                "1.2.840.113619.2.55.3.604688119.969.1364202859.76"
        ));
        info.put("sampleStudyUidsNote", "These are example UIDs for testing. Replace with actual UIDs from downloaded data.");

        return ResponseEntity.ok(info);
    }
}