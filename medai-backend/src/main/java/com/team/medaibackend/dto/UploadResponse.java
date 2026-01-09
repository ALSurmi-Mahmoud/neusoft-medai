package com.team.medaibackend.dto;

import java.util.ArrayList;
import java.util.List;

public class UploadResponse {
    private boolean success;
    private String message;
    private String studyUid;
    private Long studyId;
    private int filesProcessed;
    private int filesSuccessful;
    private int filesFailed;
    private List<FileResult> files = new ArrayList<>();

    public static class FileResult {
        private String originalFilename;
        private String storedPath;
        private String instanceUid;
        private boolean success;
        private String error;

        // Getters and Setters
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

        public String getStoredPath() { return storedPath; }
        public void setStoredPath(String storedPath) { this.storedPath = storedPath; }

        public String getInstanceUid() { return instanceUid; }
        public void setInstanceUid(String instanceUid) { this.instanceUid = instanceUid; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStudyUid() { return studyUid; }
    public void setStudyUid(String studyUid) { this.studyUid = studyUid; }

    public Long getStudyId() { return studyId; }
    public void setStudyId(Long studyId) { this.studyId = studyId; }

    public int getFilesProcessed() { return filesProcessed; }
    public void setFilesProcessed(int filesProcessed) { this.filesProcessed = filesProcessed; }

    public int getFilesSuccessful() { return filesSuccessful; }
    public void setFilesSuccessful(int filesSuccessful) { this.filesSuccessful = filesSuccessful; }

    public int getFilesFailed() { return filesFailed; }
    public void setFilesFailed(int filesFailed) { this.filesFailed = filesFailed; }

    public List<FileResult> getFiles() { return files; }
    public void setFiles(List<FileResult> files) { this.files = files; }

    public void addFileResult(FileResult result) {
        this.files.add(result);
    }
}