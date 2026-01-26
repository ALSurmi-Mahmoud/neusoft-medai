package com.team.medaibackend.exception;

/**
 * Exception thrown when a requested entity is not found in the database.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityType, Object id) {
        super(String.format("%s not found with id: %s", entityType, id));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}