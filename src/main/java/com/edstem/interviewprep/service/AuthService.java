package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.request.LoginRequest;
import com.edstem.interviewprep.dto.request.RegisterRequest;
import com.edstem.interviewprep.dto.response.TokenResponse;
import com.edstem.interviewprep.dto.response.UserResponse;
import com.edstem.interviewprep.entity.AppUser;
import com.edstem.interviewprep.entity.Role;
import com.edstem.interviewprep.exception.ConflictException;
import com.edstem.interviewprep.repository.UserRepository;
import com.edstem.interviewprep.security.TokenService;

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
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        try {
            AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), Role.USER);
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email is already registered");
        }
    }

    public TokenResponse login(LoginRequest request) {
        Optional<AppUser> user = userRepository.findByEmail(normalize(request.email()));
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        TokenService.IssuedToken token = tokenService.issue(user.get());
        return TokenResponse.bearer(token.value(), token.expiresInSeconds());
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
