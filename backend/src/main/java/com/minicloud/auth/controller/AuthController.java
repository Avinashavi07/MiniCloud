package com.minicloud.auth.controller;

import com.minicloud.auth.dto.LoginRequest;
import com.minicloud.auth.dto.LoginResponse;
import com.minicloud.auth.dto.UserResponse;
import com.minicloud.auth.service.AuthenticationService;
import com.minicloud.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authenticationService.login(request), "Authenticated");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        authenticationService.logout(authentication.getName());
        return ApiResponse.ok(null, "Logged out");
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        return ApiResponse.ok(authenticationService.currentUser(authentication.getName()));
    }
}
