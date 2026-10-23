package com.minicloud.network.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.infrastructure.network.VirtualNetworkInfo;
import com.minicloud.network.dto.AutostartRequestDto;
import com.minicloud.network.dto.CreateNetworkRequestDto;
import com.minicloud.network.dto.NetworkListResponse;
import com.minicloud.network.service.NetworkService;
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
@RequestMapping("/api/v1/networks")
public class NetworkController {

    private final NetworkService networkService;

    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<NetworkListResponse> list() {
        NetworkListResponse data = networkService.list();
        return ApiResponse.ok(data, data.note());
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<VirtualNetworkInfo> get(@PathVariable String name) {
        return ApiResponse.ok(networkService.get(name), "Network read from libvirt");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualNetworkInfo> create(@Valid @RequestBody CreateNetworkRequestDto request, Authentication authentication) {
        return ApiResponse.ok(networkService.create(request, authentication.getName()), "Network created in libvirt");
    }

    @PostMapping("/{name}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualNetworkInfo> start(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(networkService.start(name, authentication.getName()), "Network started successfully");
    }

    @PostMapping("/{name}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualNetworkInfo> stop(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(networkService.stop(name, authentication.getName()), "Network stopped successfully");
    }

    @PostMapping("/{name}/autostart")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualNetworkInfo> autostart(
            @PathVariable String name,
            @RequestBody(required = false) AutostartRequestDto request,
            Authentication authentication
    ) {
        boolean enabled = request == null || request.isAutostart();
        return ApiResponse.ok(networkService.setAutostart(name, enabled, authentication.getName()), "Network autostart updated");
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> delete(@PathVariable String name, Authentication authentication) {
        networkService.delete(name, authentication.getName());
        return ApiResponse.ok(null, "Network deleted from libvirt");
    }
}
