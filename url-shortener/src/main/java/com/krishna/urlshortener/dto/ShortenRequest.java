package com.krishna.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * What the client sends when creating a short URL.
 * POST /api/shorten
 * { "originalUrl": "https://example.com/very/long/path", "expiryDays": 30 }
 */
@Data
public class ShortenRequest {

    @NotBlank(message = "originalUrl must not be empty")
    @Pattern(regexp = "^https?://.+", message = "originalUrl must start with http:// or https://")
    private String originalUrl;

    // Optional - if not provided, service uses app.default-expiry-days
    private Integer expiryDays;
}
