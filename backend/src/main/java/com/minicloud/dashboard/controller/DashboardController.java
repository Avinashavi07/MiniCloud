package com.minicloud.dashboard.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.dashboard.dto.DashboardSummary;
import com.minicloud.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<DashboardSummary> dashboard() {
        return ApiResponse.ok(dashboardService.summarize(), "Dashboard built from live host and hypervisor probes");
    }
}
