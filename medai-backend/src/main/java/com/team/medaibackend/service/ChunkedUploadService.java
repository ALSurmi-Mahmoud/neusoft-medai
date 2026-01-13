package com.team.medaibackend.service;

import com.team.medaibackend.dto.ChunkedUploadResponse;
import com.team.medaibackend.model.UploadSession;
import com.team.medaibackend.storage.LocalFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChunkedUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkedUploadService.class);

    // In-memory session storage (for prototype)
    // Production: use Redis or database
    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    private final LocalFileStorage fileStorage;
    private final UploadService uploadService;

    public ChunkedUploadService(LocalFileStorage fileStorage, UploadService uploadService) {
        this.fileStorage = fileStorage;
        this.uploadService = uploadService;
    }

    /**
     * Initialize upload session
     */
    public ChunkedUploadResponse initUpload(String filename, Long fileSize,
                                            Integer totalChunks, Integer chunkSize,
                                            String sha256) {
        String uploadId = UUID.randomUUID().toString();

        UploadSession session = new UploadSession();
        session.setUploadId(uploadId);
        session.setFilename(filename);
        session.setFileSize(fileSize);
        session.setTotalChunks(totalChunks);
        session.setChunkSize(chunkSize);
        session.setExpectedSha256(sha256);

        sessions.put(uploadId, session);

        // Create chunk storage directory
        try {
            Path chunkDir = getChunkDirectory(uploadId);
            Files.createDirectories(chunkDir);
            logger.info("Created chunk directory for upload: {}", uploadId);
        } catch (IOException e) {
            logger.error("Failed to create chunk directory", e);
            return ChunkedUploadResponse.error("Failed to initialize upload");
        }

        ChunkedUploadResponse response = new ChunkedUploadResponse();
        response.setUploadId(uploadId);
        response.setStatus("initialized");
        response.setMessage("Upload session created");
        response.setTotalChunks(totalChunks);
        response.setProgress(0.0);

        return response;
    }

    /**
     * Upload a single chunk
     */
    public ChunkedUploadResponse uploadChunk(String uploadId, MultipartFile file,
                                             Integer chunkIndex, Integer totalChunks,
                                             String sha256) {
        UploadSession session = sessions.get(uploadId);

        if (session == null) {
            return ChunkedUploadResponse.error("Upload session not found");
        }

        try {
            // Save chunk to temporary location
            Path chunkPath = getChunkPath(uploadId, chunkIndex);
            file.transferTo(chunkPath);

            // Mark chunk as received
            session.getReceivedChunks().add(chunkIndex);
            session.setLastActivityAt(java.time.LocalDateTime.now());
            session.setStatus("uploading");

            logger.info("Received chunk {}/{} for upload {}",
                    chunkIndex + 1, totalChunks, uploadId);

            ChunkedUploadResponse response = new ChunkedUploadResponse();
            response.setUploadId(uploadId);
            response.setStatus("chunk_received");
            response.setChunkIndex(chunkIndex);
            response.setChunksReceived(session.getReceivedChunks().size());
            response.setTotalChunks(totalChunks);
            response.setProgress(session.getProgress());
            response.setMessage(String.format("Chunk %d/%d received",
                    chunkIndex + 1, totalChunks));

            return response;

        } catch (IOException e) {
            logger.error("Failed to save chunk", e);
            return ChunkedUploadResponse.error("Failed to save chunk: " + e.getMessage());
        }
    }

    /**
     * Complete upload: assemble chunks, validate checksum, process file
     */
    public ChunkedUploadResponse completeUpload(String uploadId, String expectedSha256) {
        UploadSession session = sessions.get(uploadId);

        if (session == null) {
            return ChunkedUploadResponse.error("Upload session not found");
        }

        if (!session.isComplete()) {
            return ChunkedUploadResponse.error(
                    String.format("Missing chunks: received %d/%d",
                            session.getReceivedChunks().size(),
                            session.getTotalChunks())
            );
        }

        try {
            // Assemble chunks into final file
            Path finalPath = assembleChunks(session);

            // Validate checksum
            String actualSha256 = calculateSHA256(finalPath);
            String expected = expectedSha256 != null ? expectedSha256 : session.getExpectedSha256();

            boolean checksumValid = expected == null || expected.equals(actualSha256);

            if (!checksumValid) {
                logger.error("Checksum mismatch for upload {}: expected {}, got {}",
                        uploadId, expected, actualSha256);
                return ChunkedUploadResponse.error("Checksum validation failed");
            }

            // Move file to final storage location
            String storedPath = fileStorage.store(
                    Files.readAllBytes(finalPath),
                    session.getFilename(),
                    "dicom"
            );

            // Clean up chunks and temp file
            cleanupChunks(uploadId);
            Files.deleteIfExists(finalPath);

            // Update session
            session.setStatus("completed");

            logger.info("Upload {} completed successfully: {}", uploadId, storedPath);

            ChunkedUploadResponse response = new ChunkedUploadResponse();
            response.setUploadId(uploadId);
            response.setStatus("completed");
            response.setFinalPath(storedPath);
            response.setFileSize(Files.size(fileStorage.getFullPath(storedPath)));
            response.setChecksumValid(checksumValid);
            response.setProgress(100.0);
            response.setMessage("Upload completed successfully");

            // Remove session after completion
            sessions.remove(uploadId);

            return response;

        } catch (Exception e) {
            logger.error("Failed to complete upload", e);
            return ChunkedUploadResponse.error("Failed to complete upload: " + e.getMessage());
        }
    }

    /**
     * Cancel upload and clean up
     */
    public void cancelUpload(String uploadId) {
        sessions.remove(uploadId);
        cleanupChunks(uploadId);
        logger.info("Upload {} cancelled", uploadId);
    }

    /**
     * Get upload status
     */
    public ChunkedUploadResponse getUploadStatus(String uploadId) {
        UploadSession session = sessions.get(uploadId);

        if (session == null) {
            return null;
        }

        ChunkedUploadResponse response = new ChunkedUploadResponse();
        response.setUploadId(uploadId);
        response.setStatus(session.getStatus());
        response.setChunksReceived(session.getReceivedChunks().size());
        response.setTotalChunks(session.getTotalChunks());
        response.setProgress(session.getProgress());

        return response;
    }

    // Helper methods

    private Path getChunkDirectory(String uploadId) {
        return Paths.get(fileStorage.getRootPath(), "chunks", uploadId);
    }

    private Path getChunkPath(String uploadId, Integer chunkIndex) {
        return getChunkDirectory(uploadId).resolve(String.format("chunk_%d", chunkIndex));
    }

    private Path assembleChunks(UploadSession session) throws IOException {
        Path tempFile = Files.createTempFile("upload_", "_" + session.getFilename());

        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            for (int i = 0; i < session.getTotalChunks(); i++) {
                Path chunkPath = getChunkPath(session.getUploadId(), i);
                Files.copy(chunkPath, fos);
            }
        }

        logger.info("Assembled {} chunks into {}", session.getTotalChunks(), tempFile);
        return tempFile;
    }

    private String calculateSHA256(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private void cleanupChunks(String uploadId) {
        try {
            Path chunkDir = getChunkDirectory(uploadId);
            if (Files.exists(chunkDir)) {
                Files.walk(chunkDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.warn("Failed to delete {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            logger.error("Failed to cleanup chunks for upload {}", uploadId, e);
        }
    }
}