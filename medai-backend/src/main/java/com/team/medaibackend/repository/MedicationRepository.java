package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    // Search by name (case-insensitive, partial match)
    @Query("SELECT m FROM Medication m WHERE " +
            "LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.genericName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.brandName) LIKE LOWER(CONCAT('%', :search, '%')) AND m.active = true")
    List<Medication> searchByName(@Param("search") String search);

    // Find active medications only
    List<Medication> findByActiveTrue();

    // Find by drug class
    List<Medication> findByDrugClassAndActiveTrue(String drugClass);

    // Find by generic name
    List<Medication> findByGenericNameAndActiveTrue(String genericName);

    // Search with limit for autocomplete
    @Query(value = "SELECT m FROM Medication m WHERE " +
            "(LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.genericName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.brandName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND m.active = true ORDER BY " +
            "CASE WHEN LOWER(m.name) = LOWER(:search) THEN 1 " +
            "WHEN LOWER(m.name) LIKE LOWER(CONCAT(:search, '%')) THEN 2 " +
            "ELSE 3 END")
    List<Medication> searchForAutocomplete(@Param("search") String search);
}