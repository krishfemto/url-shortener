package com.krishna.urlshortener.controller;

import com.krishna.urlshortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles the actual redirect: when someone visits http://localhost:8080/aB3xY9z,
 * this controller looks up the original URL and redirects the browser there.
 *
 * Separate from UrlShortenerController because this lives at the ROOT path "/"
 * (not under "/api") - this is how real short links work, e.g. bit.ly/aB3xY9z
 */
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService service;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = service.resolveUrl(shortCode);

        // HTTP 302 Found = temporary redirect. Browser follows the Location header.
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
