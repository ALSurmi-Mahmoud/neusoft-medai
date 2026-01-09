package com.team.medaibackend.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for file storage operations.
 * Implementations can include local filesystem, MinIO, S3, etc.
 */
public interface FileStorage {

    /**
     * Initialize the storage system (create directories, etc.)
     */
    void init() throws IOException;

    /**
     * Store a file from MultipartFile
     * @param file The uploaded file
     * @param subDirectory Optional subdirectory within storage root
     * @return The relative path where the file was stored
     */
    String store(MultipartFile file, String subDirectory) throws IOException;

    /**
     * Store a file from InputStream
     * @param inputStream The file content
     * @param filename The target filename
     * @param subDirectory Optional subdirectory within storage root
     * @return The relative path where the file was stored
     */
    String store(InputStream inputStream, String filename, String subDirectory) throws IOException;

    /**
     * Store raw bytes
     * @param content The file content as bytes
     * @param filename The target filename
     * @param subDirectory Optional subdirectory within storage root
     * @return The relative path where the file was stored
     */
    String store(byte[] content, String filename, String subDirectory) throws IOException;

    /**
     * Load a file as InputStream
     * @param relativePath The relative path to the file
     * @return InputStream of the file content
     */
    InputStream load(String relativePath) throws IOException;

    /**
     * Load a file as byte array
     * @param relativePath The relative path to the file
     * @return The file content as bytes
     */
    byte[] loadAsBytes(String relativePath) throws IOException;

    /**
     * Get the full path to a stored file
     * @param relativePath The relative path to the file
     * @return The absolute Path object
     */
    Path getFullPath(String relativePath);

    /**
     * Check if a file exists
     * @param relativePath The relative path to the file
     * @return true if file exists
     */
    boolean exists(String relativePath);

    /**
     * Delete a file
     * @param relativePath The relative path to the file
     * @return true if deletion was successful
     */
    boolean delete(String relativePath) throws IOException;

    /**
     * Delete a directory and all its contents
     * @param relativeDirectory The relative path to the directory
     * @return true if deletion was successful
     */
    boolean deleteDirectory(String relativeDirectory) throws IOException;

    /**
     * List files in a directory
     * @param relativeDirectory The relative path to the directory
     * @return List of relative paths to files in the directory
     */
    List<String> listFiles(String relativeDirectory) throws IOException;

    /**
     * Get the file size in bytes
     * @param relativePath The relative path to the file
     * @return File size in bytes
     */
    long getFileSize(String relativePath) throws IOException;

    /**
     * Get the storage root path
     * @return The root path as string
     */
    String getRootPath();
}