package com.edstem.interviewprep.shortener;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ShortUrlController {

    static final String REDIRECT_PREFIX = "/r/";

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<ShortUrlResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortUrl created = shortUrlService.shorten(request);
        URI shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(REDIRECT_PREFIX + created.getCode())
                .build().toUri();
        ShortUrlResponse body = new ShortUrlResponse(created.getCode(), shortUrl.toString(),
                created.getOriginalUrl(), created.getExpiresAt(), created.getCreatedAt());
        return ResponseEntity.created(shortUrl).body(body);
    }

    @GetMapping(REDIRECT_PREFIX + "{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String target = shortUrlService.resolveAndCountVisit(code);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    @GetMapping("/api/urls/{code}/stats")
    public ShortUrlStatsResponse stats(@PathVariable String code) {
        return shortUrlService.stats(code);
    }
}
