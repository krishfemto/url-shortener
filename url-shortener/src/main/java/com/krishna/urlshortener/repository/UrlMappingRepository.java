package com.krishna.urlshortener.repository;

import com.krishna.urlshortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository.
 *
 * Just by extending JpaRepository, we automatically get:
 *   save(), findById(), findAll(), delete(), deleteAll(), etc.
 *
 * Below we add a few CUSTOM queries for things JPA can't auto-generate.
 */
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    // Spring Data JPA auto-generates this from the method name:
    // "SELECT * FROM url_mappings WHERE short_code = ?"
    Optional<UrlMapping> findByShortCode(String shortCode);

    // Auto-generated: "SELECT * FROM url_mappings WHERE expires_at < ?"
    // Used by the cleanup job to find expired URLs
    List<UrlMapping> findByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Atomically increments the click_count column by 1.
     *
     * Why not just: mapping.setClickCount(mapping.getClickCount() + 1); save(mapping);
     * -> Because that approach has a RACE CONDITION:
     *    If 2 requests read clickCount=5 at the same time, both calculate 6,
     *    and both save 6 -> we LOSE one click.
     *
     * This query runs the increment directly in the database (UPDATE ... SET count = count + 1),
     * which is atomic and safe under concurrent traffic.
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);
}
