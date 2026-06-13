package com.krishna.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the URL Shortener application.
 *
 * @EnableAsync     -> allows methods annotated with @Async to run on a separate thread
 *                      (used for click-count updates so redirects aren't slowed down)
 * @EnableScheduling -> allows @Scheduled methods to run automatically
 *                      (used for the daily cleanup job that deletes expired URLs)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
