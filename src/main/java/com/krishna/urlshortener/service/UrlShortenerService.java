package com.krishna.urlshortener.service;

import com.krishna.urlshortener.entity.UrlMapping;
import com.krishna.urlshortener.exception.UrlNotFoundException;
import com.krishna.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core business logic for the URL Shortener.
 *
 * CACHING NOTE:
 * In a production system with multiple servers, you'd use Redis so all
 * servers share the same cache. For a single-server app like this one,
 * a ConcurrentHashMap in memory does the same job - fast lookups without
 * hitting the database every time - with zero extra infrastructure.
 * The tradeoff: this cache is per-instance and clears on restart, whereas
 * Redis would be shared and persistent across restarts.
 */
@Service
@RequiredArgsConstructor
@EnableAsync
public class UrlShortenerService {

    private final UrlMappingRepository repository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.default-expiry-days}")
    private int defaultExpiryDays;

    private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    // In-memory cache: shortCode -> originalUrl
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Creates a new short URL.
     */
    public UrlMapping shorten(String originalUrl) {
        String shortCode = generateUniqueShortCode();

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(defaultExpiryDays))
                .build();

        repository.save(mapping);
        cache.put(shortCode, originalUrl);

        return mapping;
    }

    /**
     * Resolves a short code to its original URL.
     * Checks the in-memory cache first, falls back to the database.
     */
    public String resolveUrl(String shortCode) {
        String cached = cache.get(shortCode);
        if (cached != null) {
            incrementClickAsync(shortCode);
            return cached;
        }

        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        cache.put(shortCode, mapping.getOriginalUrl());
        incrementClickAsync(shortCode);
        return mapping.getOriginalUrl();
    }

    /**
     * Updates the click count on a background thread so redirects
     * aren't slowed down waiting for a database write.
     */
    @Async
    public void incrementClickAsync(String shortCode) {
        repository.incrementClickCount(shortCode);
    }

    /**
     * Returns the most recently created links, for display on the homepage.
     */
    public List<UrlMapping> getRecentLinks() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }

    public String buildShortUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }

    /**
     * Generates a random 7-character Base62 code, retrying on the rare
     * chance of a collision with an existing code.
     */
    private String generateUniqueShortCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
            for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
                sb.append(BASE62_CHARS.charAt(RANDOM.nextInt(BASE62_CHARS.length())));
            }
            code = sb.toString();
        } while (repository.findByShortCode(code).isPresent());

        return code;
    }
}
