package com.krishna.urlshortener.service;

import com.krishna.urlshortener.entity.UrlMapping;
import com.krishna.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background job that deletes expired short URLs.
 *
 * @Scheduled(cron = "...") tells Spring to run this method automatically
 * on a schedule - no manual trigger needed.
 *
 * Cron format: "second minute hour day-of-month month day-of-week"
 * "0 0 2 * * *" = run at 02:00:00 every day
 *
 * (For TESTING, you can temporarily change this to "0 * * * * *" to run every minute)
 */
@Component
@RequiredArgsConstructor
@Slf4j  // Lombok: gives us a 'log' object for free (log.info, log.error, etc.)
public class UrlCleanupJob {

    private final UrlMappingRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "url:";

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteExpiredUrls() {
        List<UrlMapping> expired = repository.findByExpiresAtBefore(LocalDateTime.now());

        if (expired.isEmpty()) {
            log.info("Cleanup job ran - no expired URLs found");
            return;
        }

        for (UrlMapping mapping : expired) {
            // Remove from cache too, so we don't serve a "deleted" URL from Redis
            redisTemplate.delete(CACHE_PREFIX + mapping.getShortCode());
        }

        repository.deleteAll(expired);
        log.info("Cleanup job deleted {} expired URL(s)", expired.size());
    }
}
