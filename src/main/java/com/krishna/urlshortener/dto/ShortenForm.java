package com.krishna.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Bound to the HTML form fields on the homepage.
 */
@Data
public class ShortenForm {

    @NotBlank(message = "Please enter a URL")
    @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
    private String originalUrl;
}
