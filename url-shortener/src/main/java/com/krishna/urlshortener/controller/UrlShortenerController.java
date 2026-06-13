package com.krishna.urlshortener.controller;

import com.krishna.urlshortener.dto.AnalyticsResponse;
import com.krishna.urlshortener.dto.ShortenRequest;
import com.krishna.urlshortener.dto.ShortenResponse;
import com.krishna.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Main API for creating short URLs and viewing analytics.
 *
 * Base path: /api
 *   POST /api/shorten          -> create a new short URL
 *   GET  /api/analytics/{code} -> view click stats for a short URL
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService service;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortenResponse response = service.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        return ResponseEntity.ok(service.getAnalytics(shortCode));
    }
}
