package com.edstem.interviewprep.controller;

import com.edstem.interviewprep.dto.response.UserResponse;
import com.edstem.interviewprep.service.UserService;

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

    @GetMapping("/api/users/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getByEmail(jwt.getSubject());
    }

    @GetMapping("/api/admin/users")
    public List<UserResponse> listUsers() {
        return userService.listAll();
    }
}
