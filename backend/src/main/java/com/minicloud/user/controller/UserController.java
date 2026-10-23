package com.minicloud.user.controller;

import com.minicloud.auth.dto.UserResponse;
import com.minicloud.auth.repository.UserRepository;
import com.minicloud.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> list() {
        List<UserResponse> users = userRepository.findAll().stream().map(UserResponse::from).toList();
        return ApiResponse.ok(users);
    }
}
