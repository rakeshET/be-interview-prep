package com.edstem.interviewprep.common.error;

/**
 * The request clashes with the current state (duplicate email, not enough stock, ...) - maps to 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
