package com.edstem.interviewprep.auth;

import com.edstem.interviewprep.common.error.ConflictException;
import com.edstem.interviewprep.security.TokenService;
import com.edstem.interviewprep.user.AppUser;
import com.edstem.interviewprep.user.Role;
import com.edstem.interviewprep.user.UserRepository;
import com.edstem.interviewprep.user.UserResponse;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    // Compared against when the email is unknown, so "no such user" takes as long as "wrong password"
    // and response timing doesn't reveal which emails are registered.
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /** Public registration always creates a USER; ADMINs are only created by {@code AdminUserSeeder}. */
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        try {
            AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), Role.USER);
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Two registrations with the same email raced past the exists-check; the unique constraint caught it.
            throw new ConflictException("Email is already registered");
        }
    }

    public TokenResponse login(LoginRequest request) {
        Optional<AppUser> user = userRepository.findByEmail(normalize(request.email()));
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            // Same message for both cases: don't tell an attacker which emails exist.
            throw new BadCredentialsException("Invalid email or password");
        }
        TokenService.IssuedToken token = tokenService.issue(user.get());
        return TokenResponse.bearer(token.value(), token.expiresInSeconds());
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
