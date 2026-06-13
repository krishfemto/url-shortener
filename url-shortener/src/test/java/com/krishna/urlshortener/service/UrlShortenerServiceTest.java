package com.krishna.urlshortener.service;

import com.krishna.urlshortener.dto.ShortenRequest;
import com.krishna.urlshortener.dto.ShortenResponse;
import com.krishna.urlshortener.entity.UrlMapping;
import com.krishna.urlshortener.exception.UrlExpiredException;
import com.krishna.urlshortener.exception.UrlNotFoundException;
import com.krishna.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlShortenerService.
 *
 * "Unit" test = we test the service in ISOLATION, replacing its dependencies
 * (the database repository and Redis) with FAKE versions (Mockito @Mock).
 *
 * This lets us test the BUSINESS LOGIC (e.g. "throw exception if expired")
 * without needing a real database or Redis running.
 */
@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        // Inject @Value properties manually since there's no Spring context in unit tests
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "defaultExpiryDays", 30);

        // Whenever the service calls redisTemplate.opsForValue(), return our mock
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shorten_shouldCreateMappingAndCacheIt() {
        // Arrange
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com/some/long/path");

        // findByShortCode returns empty -> means generated code is unique on first try
        when(repository.findByShortCode(anyString())).thenReturn(Optional.empty());

        // Act
        ShortenResponse response = service.shorten(request);

        // Assert
        assertNotNull(response.getShortCode());
        assertEquals(7, response.getShortCode().length());
        assertEquals("https://example.com/some/long/path", response.getOriginalUrl());
        assertTrue(response.getShortUrl().startsWith("http://localhost:8080/"));

        // Verify the mapping was saved to the database
        verify(repository, times(1)).save(any(UrlMapping.class));

        // Verify the URL was cached in Redis
        verify(valueOperations, times(1))
                .set(anyString(), eq("https://example.com/some/long/path"), any());
    }

    @Test
    void resolveUrl_cacheHit_shouldReturnFromRedisWithoutQueryingDb() {
        // Arrange: Redis already has this short code cached
        when(valueOperations.get("url:abc1234")).thenReturn("https://cached-example.com");

        // Act
        String result = service.resolveUrl("abc1234");

        // Assert
        assertEquals("https://cached-example.com", result);

        // Database should NOT be queried on a cache hit
        verify(repository, never()).findByShortCode(anyString());
    }

    @Test
    void resolveUrl_cacheMiss_shouldFallBackToDatabase() {
        // Arrange: Redis has nothing (cache miss)
        when(valueOperations.get("url:xyz9999")).thenReturn(null);

        UrlMapping mapping = UrlMapping.builder()
                .shortCode("xyz9999")
                .originalUrl("https://from-database.com")
                .clickCount(5L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .build();

        when(repository.findByShortCode("xyz9999")).thenReturn(Optional.of(mapping));

        // Act
        String result = service.resolveUrl("xyz9999");

        // Assert
        assertEquals("https://from-database.com", result);

        // After a cache miss, the service should re-populate Redis
        verify(valueOperations, times(1))
                .set(eq("url:xyz9999"), eq("https://from-database.com"), any());
    }

    @Test
    void resolveUrl_expiredUrl_shouldThrowException() {
        when(valueOperations.get("url:old0000")).thenReturn(null);

        UrlMapping expiredMapping = UrlMapping.builder()
                .shortCode("old0000")
                .originalUrl("https://old-link.com")
                .clickCount(100L)
                .createdAt(LocalDateTime.now().minusDays(60))
                .expiresAt(LocalDateTime.now().minusDays(1)) // expired yesterday
                .build();

        when(repository.findByShortCode("old0000")).thenReturn(Optional.of(expiredMapping));

        // Act + Assert
        assertThrows(UrlExpiredException.class, () -> service.resolveUrl("old0000"));
    }

    @Test
    void resolveUrl_nonExistentCode_shouldThrowNotFound() {
        when(valueOperations.get("url:notexist")).thenReturn(null);
        when(repository.findByShortCode("notexist")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.resolveUrl("notexist"));
    }

    // Helper for matching Duration args (any() with generics can be ambiguous)
    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
