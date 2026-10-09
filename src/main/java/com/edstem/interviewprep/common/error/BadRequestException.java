package com.edstem.interviewprep.common.error;

/**
 * Invalid input that Bean Validation can't express (e.g. an unknown sort field) - maps to 400
 * with a field-level error, like any other validation failure.
 */
public class BadRequestException extends RuntimeException {

    private final String field;

    public BadRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
