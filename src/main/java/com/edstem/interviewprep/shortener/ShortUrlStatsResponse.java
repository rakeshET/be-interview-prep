package com.edstem.interviewprep.shortener;

import java.time.Instant;

public record ShortUrlStatsResponse(
        String code,
        String originalUrl,
        long visitCount,
        Instant createdAt,
        Instant expiresAt) {

    static ShortUrlStatsResponse from(ShortUrl shortUrl) {
        return new ShortUrlStatsResponse(shortUrl.getCode(), shortUrl.getOriginalUrl(), shortUrl.getVisitCount(),
                shortUrl.getCreatedAt(), shortUrl.getExpiresAt());
    }
}
