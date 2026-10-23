package com.minicloud.user.controller;

import com.minicloud.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(List.of(
                Map.of(
                        "name", "ADMIN",
                        "description", "Full control of the MiniCloud control plane",
                        "permissions", List.of("manage-users", "view-infrastructure", "manage-vms", "manage-networks", "manage-storage")
                ),
                Map.of(
                        "name", "OPERATOR",
                        "description", "Operate infrastructure. Cannot manage users.",
                        "permissions", List.of("view-infrastructure", "manage-vms", "manage-networks", "manage-storage")
                ),
                Map.of(
                        "name", "VIEWER",
                        "description", "Read-only access to discovered infrastructure",
                        "permissions", List.of("view-infrastructure")
                )
        ));
    }
}
