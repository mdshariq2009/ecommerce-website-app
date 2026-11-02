package com.ecommerce.exception;

public class TooManyLoginAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super("Too many failed attempts. Please wait for 1 minute before trying again.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
