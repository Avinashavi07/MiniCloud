package com.minicloud.hypervisor.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hypervisor")
public class HypervisorController {

    private final HypervisorProvider hypervisorProvider;

    public HypervisorController(HypervisorProvider hypervisorProvider) {
        this.hypervisorProvider = hypervisorProvider;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<HypervisorCapabilities> status() {
        return ApiResponse.ok(hypervisorProvider.capabilities(), "Live libvirt probe");
    }
}
