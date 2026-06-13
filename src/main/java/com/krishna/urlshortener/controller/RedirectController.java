package com.krishna.urlshortener.controller;

import com.krishna.urlshortener.exception.UrlNotFoundException;
import com.krishna.urlshortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles the actual redirect when someone visits a short link,
 * e.g. http://localhost:8080/aB3xY9z
 */
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService service;

    @GetMapping("/{shortCode:[a-zA-Z0-9]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = service.resolveUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<String> handleNotFound(UrlNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<h2>Link not found</h2><p>" + ex.getMessage() + "</p><a href='/'>Go home</a>");
    }
}
