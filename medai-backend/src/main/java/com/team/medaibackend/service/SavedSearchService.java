package com.team.medaibackend.service;

import com.team.medaibackend.entity.SavedSearch;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.SavedSearchRepository;
import com.team.medaibackend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing saved searches
 */
@Service
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final SecurityUtils securityUtils;
    private final SearchService searchService;
    private final AuditService auditService;

    public SavedSearchService(
            SavedSearchRepository savedSearchRepository,
            SecurityUtils securityUtils,
            SearchService searchService,
            AuditService auditService) {
        this.savedSearchRepository = savedSearchRepository;
        this.securityUtils = securityUtils;
        this.searchService = searchService;
        this.auditService = auditService;
    }

    /**
     * Get all saved searches for current user
     */
    @Transactional(readOnly = true)
    public List<SavedSearch> getUserSavedSearches() {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        return savedSearchRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId());
    }

    /**
     * Get accessible saved searches (own + public)
     */
    @Transactional(readOnly = true)
    public List<SavedSearch> getAccessibleSearches() {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        return savedSearchRepository.findAccessibleSearches(currentUser.getId());
    }

    /**
     * Get quick filters
     */
    @Transactional(readOnly = true)
    public List<SavedSearch> getQuickFilters(String entityType) {
        if (entityType != null && !entityType.isEmpty()) {
            return savedSearchRepository.findByIsQuickFilterTrueAndEntityTypeOrderByNameAsc(entityType);
        }
        return savedSearchRepository.findByIsQuickFilterTrueOrderByNameAsc();
    }

    /**
     * Get saved search by ID
     */
    @Transactional(readOnly = true)
    public SavedSearch getSavedSearchById(Long id) {
        return savedSearchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Saved search not found: " + id));
    }

    /**
     * Create new saved search
     */
    @Transactional
    public SavedSearch createSavedSearch(SavedSearch savedSearch) {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        // Check if name already exists for user
        if (savedSearchRepository.existsByNameAndCreatedById(
                savedSearch.getName(), currentUser.getId())) {
            throw new RuntimeException("A saved search with this name already exists");
        }

        savedSearch.setCreatedBy(currentUser);
        savedSearch.setIsQuickFilter(false);
        savedSearch.setExecutionCount(0);

        SavedSearch saved = savedSearchRepository.save(savedSearch);

        auditService.log(
                AuditService.ACTION_CREATE,
                "SAVED_SEARCH",
                saved.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    /**
     * Update saved search
     */
    @Transactional
    public SavedSearch updateSavedSearch(Long id, SavedSearch updates) {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        SavedSearch existing = getSavedSearchById(id);

        // Only owner can update
        if (!existing.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to update this saved search");
        }

        // System quick filters cannot be modified
        if (existing.getIsQuickFilter() && existing.getQuickFilterType() != null) {
            throw new RuntimeException("System quick filters cannot be modified");
        }

        // Update fields
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getSearchQuery() != null) {
            existing.setSearchQuery(updates.getSearchQuery());
        }
        if (updates.getFilters() != null) {
            existing.setFilters(updates.getFilters());
        }
        if (updates.getIsPublic() != null) {
            existing.setIsPublic(updates.getIsPublic());
        }

        SavedSearch saved = savedSearchRepository.save(existing);

        auditService.log(
                AuditService.ACTION_UPDATE,
                "SAVED_SEARCH",
                saved.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );

        return saved;
    }

    /**
     * Delete saved search
     */
    @Transactional
    public void deleteSavedSearch(Long id) {
        User currentUser = securityUtils.getCurrentUserOrThrow();

        SavedSearch savedSearch = getSavedSearchById(id);

        // Only owner can delete
        if (!savedSearch.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to delete this saved search");
        }

        // System quick filters cannot be deleted
        if (savedSearch.getIsQuickFilter() && savedSearch.getQuickFilterType() != null) {
            throw new RuntimeException("System quick filters cannot be deleted");
        }

        savedSearchRepository.delete(savedSearch);

        auditService.log(
                AuditService.ACTION_DELETE,
                "SAVED_SEARCH",
                savedSearch.getId().toString(),
                currentUser.getId(),
                currentUser.getUsername()
        );
    }

    /**
     * Execute saved search
     */
    @Transactional
    public Object executeSavedSearch(Long id, int page, int size) {
        SavedSearch savedSearch = getSavedSearchById(id);

        // Increment execution count (done by trigger in database)
        // Just execute the search based on saved criteria

        return searchService.advancedSearch(
                savedSearch.getEntityType(),
                savedSearch.getSearchQuery(),
                savedSearch.getFilters(),
                page,
                size
        );
    }

    /**
     * Get recently used saved searches
     */
    @Transactional(readOnly = true)
    public List<SavedSearch> getRecentlyUsed(int limit) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        return savedSearchRepository.findRecentlyUsed(currentUser.getId()).stream()
                .limit(limit)
                .toList();
    }
}