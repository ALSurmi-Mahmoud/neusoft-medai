package com.team.medaibackend.storage;

import com.team.medaibackend.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorage implements FileStorage {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorage.class);

    private final StorageProperties storageProperties;
    private Path rootPath;

    public LocalFileStorage(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    @Override
    public void init() throws IOException {
        this.rootPath = Paths.get(storageProperties.getRootPath()).toAbsolutePath().normalize();

        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            logger.info("Created storage root directory: {}", rootPath);
        }

        // Create standard subdirectories
        String[] subdirs = {"dicom", "nifti", "images", "reports", "temp", "masks"};
        for (String subdir : subdirs) {
            Path subdirPath = rootPath.resolve(subdir);
            if (!Files.exists(subdirPath)) {
                Files.createDirectories(subdirPath);
                logger.info("Created storage subdirectory: {}", subdirPath);
            }
        }

        logger.info("LocalFileStorage initialized with root: {}", rootPath);
    }

    @Override
    public String store(MultipartFile file, String subDirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String filename = generateUniqueFilename(originalFilename);

        return store(file.getInputStream(), filename, subDirectory);
    }

    @Override
    public String store(InputStream inputStream, String filename, String subDirectory) throws IOException {
        Path targetDir = resolveSubDirectory(subDirectory);

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetPath = targetDir.resolve(filename).normalize();

        // Security check: ensure the target is within storage root
        if (!targetPath.startsWith(rootPath)) {
            throw new IOException("Cannot store file outside of storage root");
        }

        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = rootPath.relativize(targetPath).toString();
        logger.debug("Stored file: {}", relativePath);

        return relativePath;
    }

    @Override
    public String store(byte[] content, String filename, String subDirectory) throws IOException {
        return store(new ByteArrayInputStream(content), filename, subDirectory);
    }

    @Override
    public InputStream load(String relativePath) throws IOException {
        Path filePath = getFullPath(relativePath);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }

        return Files.newInputStream(filePath);
    }

    @Override
    public byte[] loadAsBytes(String relativePath) throws IOException {
        Path filePath = getFullPath(relativePath);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }

        return Files.readAllBytes(filePath);
    }

    @Override
    public Path getFullPath(String relativePath) {
        return rootPath.resolve(relativePath).normalize();
    }

    @Override
    public boolean exists(String relativePath) {
        Path filePath = getFullPath(relativePath);
        return Files.exists(filePath);
    }

    @Override
    public boolean delete(String relativePath) throws IOException {
        Path filePath = getFullPath(relativePath);

        if (!filePath.startsWith(rootPath)) {
            throw new IOException("Cannot delete file outside of storage root");
        }

        return Files.deleteIfExists(filePath);
    }

    @Override
    public boolean deleteDirectory(String relativeDirectory) throws IOException {
        Path dirPath = getFullPath(relativeDirectory);

        if (!dirPath.startsWith(rootPath)) {
            throw new IOException("Cannot delete directory outside of storage root");
        }

        if (!Files.exists(dirPath)) {
            return false;
        }

        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        return true;
    }

    @Override
    public List<String> listFiles(String relativeDirectory) throws IOException {
        Path dirPath = getFullPath(relativeDirectory);
        List<String> files = new ArrayList<>();

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return files;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    files.add(rootPath.relativize(entry).toString());
                }
            }
        }

        return files;
    }

    @Override
    public long getFileSize(String relativePath) throws IOException {
        Path filePath = getFullPath(relativePath);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }

        return Files.size(filePath);
    }

    @Override
    public String getRootPath() {
        return rootPath.toString();
    }

    private Path resolveSubDirectory(String subDirectory) {
        if (subDirectory == null || subDirectory.isEmpty()) {
            return rootPath;
        }
        return rootPath.resolve(subDirectory).normalize();
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }
}