package com.team.medaibackend.web;

import com.team.medaibackend.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Global search across all entities
     */
    @GetMapping("/global")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> globalSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(searchService.globalSearch(query, limit));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get search suggestions
     */
    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> getSearchSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(searchService.getSearchSuggestions(query, limit));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Advanced search with filters
     */
    @PostMapping("/advanced")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> advancedSearch(@RequestBody Map<String, Object> request) {
        try {
            String entityType = (String) request.get("entityType");
            String query = (String) request.get("query");
            @SuppressWarnings("unchecked")
            Map<String, Object> filters = (Map<String, Object>) request.get("filters");
            int page = request.containsKey("page") ?
                    ((Number) request.get("page")).intValue() : 0;
            int size = request.containsKey("size") ?
                    ((Number) request.get("size")).intValue() : 20;

            return ResponseEntity.ok(
                    searchService.advancedSearch(entityType, query, filters, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}