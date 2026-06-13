package com.krishna.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * What we return to the client after creating a short URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenResponse {
    private String shortCode;
    private String shortUrl;       // full URL e.g. http://localhost:8080/aB3xY9
    private String originalUrl;
    private LocalDateTime expiresAt;
}
