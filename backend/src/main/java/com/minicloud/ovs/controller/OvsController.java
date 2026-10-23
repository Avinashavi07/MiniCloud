package com.minicloud.ovs.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.infrastructure.ovs.OvsBridgeInfo;
import com.minicloud.ovs.dto.CreateOvsBridgeRequestDto;
import com.minicloud.ovs.dto.OvsListResponse;
import com.minicloud.ovs.service.OvsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ovs")
public class OvsController {

    private final OvsService ovsService;

    public OvsController(OvsService ovsService) {
        this.ovsService = ovsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<OvsListResponse> list() {
        OvsListResponse data = ovsService.list();
        return ApiResponse.ok(data, data.note());
    }

    @GetMapping("/bridges/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<OvsBridgeInfo> get(@PathVariable String name) {
        return ApiResponse.ok(ovsService.get(name), "OVS bridge read from ovs-vsctl");
    }

    @PostMapping("/bridges")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<OvsBridgeInfo> create(@Valid @RequestBody CreateOvsBridgeRequestDto request, Authentication authentication) {
        return ApiResponse.ok(ovsService.create(request, authentication.getName()), "OVS bridge created");
    }

    @DeleteMapping("/bridges/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> delete(@PathVariable String name, Authentication authentication) {
        ovsService.delete(name, authentication.getName());
        return ApiResponse.ok(null, "OVS bridge deleted");
    }
}
