package com.krishna.urlshortener.controller;

import com.krishna.urlshortener.dto.ShortenForm;
import com.krishna.urlshortener.entity.UrlMapping;
import com.krishna.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Serves the HTML pages (homepage with the form and link history).
 * For the JSON API equivalent, see UrlShortenerService directly.
 */
@Controller
@RequiredArgsConstructor
public class WebController {

    private final UrlShortenerService service;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("shortenForm", new ShortenForm());
        model.addAttribute("recentLinks", service.getRecentLinks());
        model.addAttribute("baseUrl", "");
        return "index";
    }

    @PostMapping("/shorten")
    public String shorten(@Valid @ModelAttribute("shortenForm") ShortenForm form,
                           BindingResult bindingResult,
                           Model model) {

        model.addAttribute("recentLinks", service.getRecentLinks());

        if (bindingResult.hasErrors()) {
            return "index";
        }

        UrlMapping mapping = service.shorten(form.getOriginalUrl());

        model.addAttribute("shortenForm", new ShortenForm());
        model.addAttribute("result", mapping);
        model.addAttribute("shortUrl", service.buildShortUrl(mapping.getShortCode()));
        // Refresh the list so the new link appears at the top
        model.addAttribute("recentLinks", service.getRecentLinks());

        return "index";
    }
}
