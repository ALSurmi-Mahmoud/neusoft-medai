package com.team.medaibackend.dto;

public class ChunkedUploadResponse {
    private String uploadId;
    private String status; // initialized, chunk_received, completed, error
    private String message;
    private Integer chunkIndex;
    private Integer chunksReceived;
    private Integer totalChunks;
    private Double progress; // percentage
    private String finalPath;
    private Long fileSize;
    private Boolean checksumValid;
    private String studyUid;

    // Constructor for error responses
    public static ChunkedUploadResponse error(String message) {
        ChunkedUploadResponse response = new ChunkedUploadResponse();
        response.setStatus("error");
        response.setMessage(message);
        return response;
    }

    // Getters and Setters
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public Integer getChunksReceived() { return chunksReceived; }
    public void setChunksReceived(Integer chunksReceived) { this.chunksReceived = chunksReceived; }

    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }

    public Double getProgress() { return progress; }
    public void setProgress(Double progress) { this.progress = progress; }

    public String getFinalPath() { return finalPath; }
    public void setFinalPath(String finalPath) { this.finalPath = finalPath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Boolean getChecksumValid() { return checksumValid; }
    public void setChecksumValid(Boolean checksumValid) { this.checksumValid = checksumValid; }

    public String getStudyUid() { return studyUid; }
    public void setStudyUid(String studyUid) { this.studyUid = studyUid; }
}