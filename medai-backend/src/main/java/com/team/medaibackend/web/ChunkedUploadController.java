// src/main/java/com/team/medaibackend/web/ChunkedUploadController.java
package com.team.medaibackend.web;

import com.team.medaibackend.dto.ChunkedUploadResponse;
import com.team.medaibackend.service.ChunkedUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class ChunkedUploadController {

    private final ChunkedUploadService chunkedUploadService;

    public ChunkedUploadController(ChunkedUploadService chunkedUploadService) {
        this.chunkedUploadService = chunkedUploadService;
    }

    @PostMapping("/init")
    public ResponseEntity<ChunkedUploadResponse> initUpload(@RequestBody Map<String, Object> request) {
        String filename = (String) request.get("filename");
        Long fileSize = request.get("fileSize") != null ? ((Number) request.get("fileSize")).longValue() : 0L;
        Integer totalChunks = request.get("totalChunks") != null ? ((Number) request.get("totalChunks")).intValue() : 0;
        Integer chunkSize = request.get("chunkSize") != null ? ((Number) request.get("chunkSize")).intValue() : 0;
        String sha256 = (String) request.get("sha256");

        if (filename == null || filename.isEmpty()) {
            return ResponseEntity.badRequest().body(ChunkedUploadResponse.error("Filename is required"));
        }
        if (totalChunks <= 0) {
            return ResponseEntity.badRequest().body(ChunkedUploadResponse.error("Invalid totalChunks"));
        }

        ChunkedUploadResponse response = chunkedUploadService.initUpload(
                filename, fileSize, totalChunks, chunkSize, sha256
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{uploadId}/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChunkedUploadResponse> uploadChunk(
            @PathVariable String uploadId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam(value = "sha256", required = false) String sha256
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ChunkedUploadResponse.error("Chunk file is empty"));
        }
        if (chunkIndex == null || chunkIndex < 0) {
            return ResponseEntity.badRequest().body(ChunkedUploadResponse.error("Invalid chunkIndex"));
        }

        ChunkedUploadResponse response = chunkedUploadService.uploadChunk(
                uploadId, file, chunkIndex, totalChunks, sha256
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<ChunkedUploadResponse> completeUpload(
            @PathVariable String uploadId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String expectedSha256 = request != null ? request.get("sha256") : null;

        ChunkedUploadResponse response = chunkedUploadService.completeUpload(uploadId, expectedSha256);

        if ("completed".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Map<String, Object>> cancelUpload(@PathVariable String uploadId) {
        chunkedUploadService.cancelUpload(uploadId);

        Map<String, Object> response = new HashMap<>();
        response.put("uploadId", uploadId);
        response.put("status", "cancelled");
        response.put("message", "Upload cancelled and chunks deleted");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uploadId}/status")
    public ResponseEntity<ChunkedUploadResponse> getUploadStatus(@PathVariable String uploadId) {
        ChunkedUploadResponse response = chunkedUploadService.getUploadStatus(uploadId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
}
