package com.edstem.interviewprep.shortener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "short_urls", uniqueConstraints = @UniqueConstraint(name = "uk_short_urls_code", columnNames = "code"))
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The unique constraint (which also gives us the lookup index) is the real guarantee of uniqueness.
    @Column(nullable = false, length = 8)
    private String code;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    private Instant expiresAt;

    // Only ever changed by an atomic UPDATE in ShortUrlRepository, never by read-modify-write.
    @Column(nullable = false)
    private long visitCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ShortUrl() {
        // for JPA
    }

    public ShortUrl(String code, String originalUrl, Instant expiresAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getVisitCount() {
        return visitCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
