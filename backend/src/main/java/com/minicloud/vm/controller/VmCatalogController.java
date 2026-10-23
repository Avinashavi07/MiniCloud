package com.minicloud.vm.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.vm.dto.VmCatalogResponse;
import com.minicloud.vm.service.VmService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vm-catalog")
public class VmCatalogController {

    private final VmService vmService;

    public VmCatalogController(VmService vmService) {
        this.vmService = vmService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<VmCatalogResponse> catalog() {
        return ApiResponse.ok(vmService.catalog(), "Create options from the live host and libvirt");
    }
}
