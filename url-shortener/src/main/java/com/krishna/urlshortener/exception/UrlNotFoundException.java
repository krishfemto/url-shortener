package com.krishna.urlshortener.exception;

/**
 * Thrown when a short code doesn't exist in the database.
 * Caught by GlobalExceptionHandler -> returns HTTP 404.
 */
public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String shortCode) {
        super("No URL found for short code: " + shortCode);
    }
}
