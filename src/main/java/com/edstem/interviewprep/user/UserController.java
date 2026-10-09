package com.edstem.interviewprep.user;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** The caller's identity comes from the verified token's subject - never from a request parameter. */
    @GetMapping("/api/users/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getByEmail(jwt.getSubject());
    }

    /** Restricted to ADMIN by the /api/admin/** rule in SecurityConfig. */
    @GetMapping("/api/admin/users")
    public List<UserResponse> listUsers() {
        return userService.listAll();
    }
}
