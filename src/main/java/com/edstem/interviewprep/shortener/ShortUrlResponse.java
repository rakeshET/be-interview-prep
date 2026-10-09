package com.edstem.interviewprep.shortener;

import java.time.Instant;

public record ShortUrlResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant expiresAt,
        Instant createdAt) {
}
