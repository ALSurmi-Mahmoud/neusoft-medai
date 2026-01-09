package com.team.medaibackend.web;

import com.team.medaibackend.dto.UploadResponse;
import com.team.medaibackend.service.UploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Upload single DICOM/medical image file.
     *
     * Example:
     * curl -X POST http://localhost:8080/api/images/upload \
     *   -F "file=@/path/to/image.dcm"
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadSingleFile(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            UploadResponse response = new UploadResponse();
            response.setSuccess(false);
            response.setMessage("No file provided");
            return ResponseEntity.badRequest().body(response);
        }

        UploadResponse response = uploadService.uploadSingleFile(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload multiple DICOM/medical image files.
     *
     * Example:
     * curl -X POST http://localhost:8080/api/images/upload-multiple \
     *   -F "files=@/path/to/image1.dcm" \
     *   -F "files=@/path/to/image2.dcm"
     */
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            UploadResponse response = new UploadResponse();
            response.setSuccess(false);
            response.setMessage("No files provided");
            return ResponseEntity.badRequest().body(response);
        }

        UploadResponse response = uploadService.uploadFiles(files);
        return ResponseEntity.ok(response);
    }

    /**
     * Get upload status/help information.
     */
    @GetMapping("/upload-info")
    public ResponseEntity<Map<String, Object>> getUploadInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("supportedFormats", List.of(
                "DICOM (.dcm, .dicom)",
                "NIfTI (.nii, .nii.gz)",
                "JPEG (.jpg, .jpeg)",
                "PNG (.png)"
        ));

        info.put("endpoints", Map.of(
                "singleUpload", Map.of(
                        "method", "POST",
                        "url", "/api/images/upload",
                        "contentType", "multipart/form-data",
                        "parameter", "file"
                ),
                "multipleUpload", Map.of(
                        "method", "POST",
                        "url", "/api/images/upload-multiple",
                        "contentType", "multipart/form-data",
                        "parameter", "files"
                )
        ));

        info.put("maxFileSize", "500MB");
        info.put("note", "DICOM files will be parsed for metadata and stored in the database hierarchy");

        return ResponseEntity.ok(info);
    }
}