package com.minicloud.auth.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.auth.dto.LoginRequest;
import com.minicloud.auth.dto.LoginResponse;
import com.minicloud.auth.dto.UserResponse;
import com.minicloud.auth.entity.User;
import com.minicloud.auth.repository.UserRepository;
import com.minicloud.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user missing from database"));
        String token = jwtService.createToken(user.getUsername(), user.getRole());
        auditService.record(user.getUsername(), "USER_LOGIN", "USER", user.getId().toString(), "SUCCESS",
                "User logged in");
        log.info("User {} authenticated with role {}", user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", jwtService.expirationMs(), UserResponse.from(user));
    }

    @Transactional
    public void logout(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user ->
                auditService.record(username, "USER_LOGOUT", "USER", user.getId().toString(), "SUCCESS",
                        "User logged out"));
        log.info("User {} logged out (JWT remains valid until expiry in Phase 1)", username);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user missing from database"));
        return UserResponse.from(user);
    }
}
