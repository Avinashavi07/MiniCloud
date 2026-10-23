package com.minicloud.storage.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.infrastructure.storage.StorageVolume;
import com.minicloud.infrastructure.storage.VirtualStoragePool;
import com.minicloud.storage.dto.AutostartRequestDto;
import com.minicloud.storage.dto.CreatePoolRequestDto;
import com.minicloud.storage.dto.CreateVolumeRequestDto;
import com.minicloud.storage.dto.StorageListResponse;
import com.minicloud.storage.service.StorageService;
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
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<StorageListResponse> list() {
        StorageListResponse data = storageService.list();
        return ApiResponse.ok(data, data.note());
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<VirtualStoragePool> get(@PathVariable String name) {
        return ApiResponse.ok(storageService.get(name), "Storage pool read from libvirt");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualStoragePool> create(@Valid @RequestBody CreatePoolRequestDto request, Authentication authentication) {
        return ApiResponse.ok(storageService.createPool(request, authentication.getName()), "Storage pool created in libvirt");
    }

    @PostMapping("/{name}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualStoragePool> start(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(storageService.start(name, authentication.getName()), "Storage pool started");
    }

    @PostMapping("/{name}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualStoragePool> stop(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(storageService.stop(name, authentication.getName()), "Storage pool stopped");
    }

    @PostMapping("/{name}/autostart")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualStoragePool> autostart(
            @PathVariable String name,
            @RequestBody(required = false) AutostartRequestDto request,
            Authentication authentication
    ) {
        boolean enabled = request == null || request.isAutostart();
        return ApiResponse.ok(storageService.setAutostart(name, enabled, authentication.getName()), "Storage pool autostart updated");
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> delete(@PathVariable String name, Authentication authentication) {
        storageService.deletePool(name, authentication.getName());
        return ApiResponse.ok(null, "Storage pool deleted from libvirt");
    }

    @PostMapping("/{name}/volumes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<StorageVolume> createVolume(
            @PathVariable String name,
            @Valid @RequestBody CreateVolumeRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(storageService.createVolume(name, request, authentication.getName()), "Volume created in libvirt");
    }

    @DeleteMapping("/{name}/volumes/{volume}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> deleteVolume(
            @PathVariable String name,
            @PathVariable String volume,
            Authentication authentication
    ) {
        storageService.deleteVolume(name, volume, authentication.getName());
        return ApiResponse.ok(null, "Volume deleted from libvirt");
    }
}
