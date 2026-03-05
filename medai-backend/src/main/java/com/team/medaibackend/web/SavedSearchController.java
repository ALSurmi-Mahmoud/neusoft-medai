package com.team.medaibackend.web;

import com.team.medaibackend.entity.SavedSearch;
import com.team.medaibackend.service.SavedSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/saved-searches")
public class SavedSearchController {

    private final SavedSearchService savedSearchService;

    public SavedSearchController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    /**
     * Get all saved searches for current user
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> getUserSavedSearches() {
        try {
            List<SavedSearch> searches = savedSearchService.getUserSavedSearches();
            List<Map<String, Object>> result = searches.stream()
                    .map(this::buildSavedSearchDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get accessible saved searches (own + public)
     */
    @GetMapping("/accessible")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> getAccessibleSearches() {
        try {
            List<SavedSearch> searches = savedSearchService.getAccessibleSearches();
            List<Map<String, Object>> result = searches.stream()
                    .map(this::buildSavedSearchDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get quick filters
     */
    @GetMapping("/quick-filters")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> getQuickFilters(
            @RequestParam(required = false) String entityType) {
        try {
            List<SavedSearch> filters = savedSearchService.getQuickFilters(entityType);
            List<Map<String, Object>> result = filters.stream()
                    .map(this::buildSavedSearchDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get saved search by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> getSavedSearch(@PathVariable Long id) {
        try {
            SavedSearch search = savedSearchService.getSavedSearchById(id);
            return ResponseEntity.ok(buildSavedSearchDto(search));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Create new saved search
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> createSavedSearch(@RequestBody Map<String, Object> request) {
        try {
            SavedSearch savedSearch = new SavedSearch();
            savedSearch.setName((String) request.get("name"));
            savedSearch.setDescription((String) request.get("description"));
            savedSearch.setEntityType((String) request.get("entityType"));
            savedSearch.setSearchQuery((String) request.get("searchQuery"));

            @SuppressWarnings("unchecked")
            Map<String, Object> filters = (Map<String, Object>) request.get("filters");
            savedSearch.setFilters(filters);

            if (request.containsKey("isPublic")) {
                savedSearch.setIsPublic((Boolean) request.get("isPublic"));
            }

            SavedSearch saved = savedSearchService.createSavedSearch(savedSearch);
            return ResponseEntity.ok(buildSavedSearchDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Update saved search
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> updateSavedSearch(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            SavedSearch updates = new SavedSearch();
            if (request.containsKey("name")) {
                updates.setName((String) request.get("name"));
            }
            if (request.containsKey("description")) {
                updates.setDescription((String) request.get("description"));
            }
            if (request.containsKey("searchQuery")) {
                updates.setSearchQuery((String) request.get("searchQuery"));
            }
            if (request.containsKey("filters")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> filters = (Map<String, Object>) request.get("filters");
                updates.setFilters(filters);
            }
            if (request.containsKey("isPublic")) {
                updates.setIsPublic((Boolean) request.get("isPublic"));
            }

            SavedSearch saved = savedSearchService.updateSavedSearch(id, updates);
            return ResponseEntity.ok(buildSavedSearchDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Delete saved search
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> deleteSavedSearch(@PathVariable Long id) {
        try {
            savedSearchService.deleteSavedSearch(id);
            return ResponseEntity.ok(Map.of("message", "Saved search deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Execute saved search
     */
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> executeSavedSearch(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(savedSearchService.executeSavedSearch(id, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get recently used searches
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<?> getRecentlyUsed(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<SavedSearch> searches = savedSearchService.getRecentlyUsed(limit);
            List<Map<String, Object>> result = searches.stream()
                    .map(this::buildSavedSearchDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> buildSavedSearchDto(SavedSearch search) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", search.getId());
        dto.put("name", search.getName());
        dto.put("description", search.getDescription());
        dto.put("entityType", search.getEntityType());
        dto.put("searchQuery", search.getSearchQuery());
        dto.put("filters", search.getFilters());
        dto.put("quickFilterType", search.getQuickFilterType());
        dto.put("isPublic", search.getIsPublic());
        dto.put("isQuickFilter", search.getIsQuickFilter());
        dto.put("executionCount", search.getExecutionCount());
        dto.put("createdAt", search.getCreatedAt());
        dto.put("updatedAt", search.getUpdatedAt());
        dto.put("lastExecutedAt", search.getLastExecutedAt());
        return dto;
    }
}