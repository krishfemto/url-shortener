package com.krishna.urlshortener.exception;

/**
 * Thrown when a short code exists but has passed its expiry date.
 * Caught by GlobalExceptionHandler -> returns HTTP 410 (Gone).
 */
public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String shortCode) {
        super("URL with short code '" + shortCode + "' has expired");
    }
}
