package com.krishna.urlshortener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents one shortened URL record in the database.
 *
 * Table created automatically by Hibernate (ddl-auto=update) based on this class.
 */
@Entity
@Table(name = "url_mappings", indexes = {
        // Index speeds up lookups by short_code (the most frequent query - redirects)
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
        // Index speeds up the daily cleanup job that searches for expired rows
        @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data               // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor   // Lombok: empty constructor (required by JPA)
@AllArgsConstructor  // Lombok: constructor with all fields
@Builder             // Lombok: lets us do UrlMapping.builder().field(x).build()
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false)
    private Long clickCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Called automatically by JPA right before the row is first saved.
     * Ensures every new record has sensible defaults even if the
     * service forgets to set them.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (clickCount == null) {
            clickCount = 0L;
        }
    }
}
