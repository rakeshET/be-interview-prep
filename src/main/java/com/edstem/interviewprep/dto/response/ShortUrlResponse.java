package com.edstem.interviewprep.dto.response;

import java.time.Instant;

public record ShortUrlResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant expiresAt,
        Instant createdAt) {
}
