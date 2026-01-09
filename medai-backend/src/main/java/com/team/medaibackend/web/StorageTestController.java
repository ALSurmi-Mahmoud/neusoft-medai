package com.team.medaibackend.web;

import com.team.medaibackend.storage.FileStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
public class StorageTestController {

    private final FileStorage fileStorage;

    public StorageTestController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("rootPath", fileStorage.getRootPath());
        info.put("status", "initialized");
        return ResponseEntity.ok(info);
    }

    @PostMapping("/test-write")
    public ResponseEntity<Map<String, Object>> testWrite() {
        Map<String, Object> result = new HashMap<>();

        try {
            String testContent = "Test file created at: " + System.currentTimeMillis();
            String filename = "test-" + System.currentTimeMillis() + ".txt";

            String storedPath = fileStorage.store(
                    testContent.getBytes(StandardCharsets.UTF_8),
                    filename,
                    "temp"
            );

            result.put("success", true);
            result.put("storedPath", storedPath);
            result.put("exists", fileStorage.exists(storedPath));
            result.put("fileSize", fileStorage.getFileSize(storedPath));

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/test-read")
    public ResponseEntity<Map<String, Object>> testRead(@RequestParam String path) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (!fileStorage.exists(path)) {
                result.put("success", false);
                result.put("error", "File not found: " + path);
                return ResponseEntity.notFound().build();
            }

            byte[] content = fileStorage.loadAsBytes(path);

            result.put("success", true);
            result.put("path", path);
            result.put("fileSize", content.length);
            result.put("content", new String(content, StandardCharsets.UTF_8));

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles(@RequestParam(defaultValue = "temp") String directory) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<String> files = fileStorage.listFiles(directory);

            result.put("success", true);
            result.put("directory", directory);
            result.put("files", files);
            result.put("count", files.size());

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @DeleteMapping("/test-delete")
    public ResponseEntity<Map<String, Object>> testDelete(@RequestParam String path) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean deleted = fileStorage.delete(path);

            result.put("success", true);
            result.put("deleted", deleted);
            result.put("path", path);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}