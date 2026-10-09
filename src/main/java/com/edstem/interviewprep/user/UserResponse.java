package com.edstem.interviewprep.user;

import java.time.Instant;

/** Public view of a user - deliberately has no password hash. */
public record UserResponse(Long id, String email, Role role, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
