package com.krishna.urlshortener.service;

import com.krishna.urlshortener.dto.AnalyticsResponse;
import com.krishna.urlshortener.dto.ShortenRequest;
import com.krishna.urlshortener.dto.ShortenResponse;
import com.krishna.urlshortener.entity.UrlMapping;
import com.krishna.urlshortener.exception.UrlExpiredException;
import com.krishna.urlshortener.exception.UrlNotFoundException;
import com.krishna.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Core business logic for the URL Shortener.
 *
 * @RequiredArgsConstructor (Lombok) -> generates a constructor for all "final" fields below.
 * This is "constructor injection" - Spring automatically passes in the repository,
 * redisTemplate, etc. when it creates this service. This is the recommended way
 * to do dependency injection (easier to test than @Autowired on fields).
 */
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.default-expiry-days}")
    private int defaultExpiryDays;

    private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Redis cache entries expire after 24 hours (data still safe in PostgreSQL)
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String CACHE_PREFIX = "url:";


    /**
     * STEP 1: Create a new short URL.
     */
    public ShortenResponse shorten(ShortenRequest request) {
        String shortCode = generateUniqueShortCode();

        int expiryDays = request.getExpiryDays() != null
                ? request.getExpiryDays()
                : defaultExpiryDays;

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(expiryDays))
                .build();

        repository.save(mapping);

        // Pre-warm the cache so the very first redirect is also fast
        redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, request.getOriginalUrl(), CACHE_TTL);

        return ShortenResponse.builder()
                .shortCode(shortCode)
                .shortUrl(baseUrl + "/" + shortCode)
                .originalUrl(mapping.getOriginalUrl())
                .expiresAt(mapping.getExpiresAt())
                .build();
    }


    /**
     * STEP 2: Resolve a short code back to the original URL (used by the redirect endpoint).
     *
     * Flow:
     *   1. Check Redis first (fast - ~1ms)
     *   2. If not in Redis (cache miss), check PostgreSQL (slower - ~10-50ms)
     *   3. If found in DB, re-populate Redis so next request is fast again
     *   4. Either way, increment the click counter ASYNCHRONOUSLY
     *      (the user doesn't wait for this - see incrementClickAsync below)
     */
    public String resolveUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            // CACHE HIT - fast path
            incrementClickAsync(shortCode);
            return cachedUrl;
        }

        // CACHE MISS - fall back to database
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException(shortCode);
        }

        // Re-populate cache for next time
        redisTemplate.opsForValue().set(cacheKey, mapping.getOriginalUrl(), CACHE_TTL);

        incrementClickAsync(shortCode);
        return mapping.getOriginalUrl();
    }


    /**
     * Increments the click counter WITHOUT blocking the redirect response.
     *
     * @Async runs this on a separate background thread (Spring's default thread pool).
     * The user gets redirected immediately; this database write happens
     * a few milliseconds later "in the background".
     *
     * Why this matters: redirects should feel instant. Writing to the
     * database on every single click would add latency to every request.
     */
    @Async
    public void incrementClickAsync(String shortCode) {
        repository.incrementClickCount(shortCode);
    }


    /**
     * STEP 3: Get analytics for a short URL.
     */
    public AnalyticsResponse getAnalytics(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return AnalyticsResponse.builder()
                .shortCode(mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .totalClicks(mapping.getClickCount())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .expired(mapping.getExpiresAt().isBefore(LocalDateTime.now()))
                .build();
    }


    /**
     * Generates a random 7-character Base62 code (letters + digits)
     * and keeps retrying if it happens to collide with an existing one.
     *
     * Base62 = 62 possible characters per position.
     * 62^7 = ~3.5 trillion possible codes - collisions are extremely rare,
     * but we check anyway because uniqueness MUST be guaranteed.
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
