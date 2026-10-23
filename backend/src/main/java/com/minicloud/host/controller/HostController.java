package com.minicloud.host.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.host.dto.HostMetricsResponse;
import com.minicloud.host.dto.HostResponse;
import com.minicloud.host.service.HostService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hosts")
public class HostController {

    private final HostService hostService;

    public HostController(HostService hostService) {
        this.hostService = hostService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<List<HostResponse>> list() {
        return ApiResponse.ok(hostService.listHosts(), "Hosts loaded from live discovery");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<HostResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(hostService.getHost(id));
    }

    @GetMapping("/{id}/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<HostMetricsResponse> metrics(@PathVariable UUID id) {
        return ApiResponse.ok(hostService.metrics(id), "Live host metrics");
    }
}
