package com.team.medaibackend.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class UploadSession {
    private String uploadId;
    private String filename;
    private Long fileSize;
    private Integer totalChunks;
    private Integer chunkSize;
    private String expectedSha256;
    private Set<Integer> receivedChunks;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private String status; // initialized, uploading, completed, error

    public UploadSession() {
        this.receivedChunks = new HashSet<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.status = "initialized";
    }

    public boolean isComplete() {
        return receivedChunks.size() == totalChunks;
    }

    public double getProgress() {
        return (receivedChunks.size() * 100.0) / totalChunks;
    }

    // Getters and Setters
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }

    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }

    public String getExpectedSha256() { return expectedSha256; }
    public void setExpectedSha256(String expectedSha256) { this.expectedSha256 = expectedSha256; }

    public Set<Integer> getReceivedChunks() { return receivedChunks; }
    public void setReceivedChunks(Set<Integer> receivedChunks) { this.receivedChunks = receivedChunks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}