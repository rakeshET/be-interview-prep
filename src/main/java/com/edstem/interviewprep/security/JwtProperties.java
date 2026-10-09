package com.edstem.interviewprep.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from {@code app.jwt.*}. The secret has no default: it comes from the {@code JWT_SECRET}
 * environment variable, and the app refuses to start without one (or with one too short for HS256).
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters (256 bits) for HS256") String secret,
        @NotNull Duration ttl,
        @NotBlank String issuer) {
}
