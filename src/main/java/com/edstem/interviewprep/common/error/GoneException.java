package com.edstem.interviewprep.common.error;

/**
 * The resource existed but is no longer available (e.g. an expired short link) - maps to 410.
 */
public class GoneException extends RuntimeException {

    public GoneException(String message) {
        super(message);
    }
}
