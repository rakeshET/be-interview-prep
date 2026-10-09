package com.edstem.interviewprep.common.error;

import java.time.Instant;
import java.util.List;

/**
 * The single JSON error shape returned by every endpoint, whatever went wrong.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }
}
