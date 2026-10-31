package com.vetclinic.common.exception;

import org.springframework.validation.Errors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A business rule failed for one or more form fields (e.g. "this phone is used by another record").
 * Services throw it; controllers call {@link #applyTo(Errors)} and show the form again with the field errors.
 * Thrown inside a {@code @Transactional} method it rolls the transaction back, which is what we want here.
 */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    public FieldValidationException(Map<String, String> fieldErrors) {
        super("Invalid fields: " + fieldErrors.keySet());
        if (fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("At least one field error is required.");
        }
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    /** Field name → user-facing message, in the order they were found. */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void applyTo(Errors errors) {
        fieldErrors.forEach((field, message) -> errors.rejectValue(field, "invalid", message));
    }
}
