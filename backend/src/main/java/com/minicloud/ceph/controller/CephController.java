package com.minicloud.ceph.controller;

import com.minicloud.ceph.dto.CephListResponse;
import com.minicloud.ceph.dto.CreateCephPoolRequestDto;
import com.minicloud.ceph.dto.CreateRbdImageRequestDto;
import com.minicloud.ceph.service.CephService;
import com.minicloud.common.response.ApiResponse;
import com.minicloud.infrastructure.ceph.CephPoolInfo;
import com.minicloud.infrastructure.ceph.RbdImageInfo;
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
@RequestMapping("/api/v1/ceph")
public class CephController {

    private final CephService cephService;

    public CephController(CephService cephService) {
        this.cephService = cephService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<CephListResponse> list() {
        CephListResponse data = cephService.list();
        return ApiResponse.ok(data, data.note());
    }

    @GetMapping("/pools/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<CephPoolInfo> getPool(@PathVariable String name) {
        return ApiResponse.ok(cephService.getPool(name), "Ceph pool read from ceph/rbd");
    }

    @PostMapping("/pools")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<CephPoolInfo> createPool(
            @Valid @RequestBody CreateCephPoolRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(cephService.createPool(request, authentication.getName()), "Ceph pool created");
    }

    @DeleteMapping("/pools/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> deletePool(@PathVariable String name, Authentication authentication) {
        cephService.deletePool(name, authentication.getName());
        return ApiResponse.ok(null, "Ceph pool deleted");
    }

    @PostMapping("/pools/{pool}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<RbdImageInfo> createImage(
            @PathVariable String pool,
            @Valid @RequestBody CreateRbdImageRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(cephService.createImage(pool, request, authentication.getName()), "RBD image created");
    }

    @DeleteMapping("/pools/{pool}/images/{image}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> deleteImage(
            @PathVariable String pool,
            @PathVariable String image,
            Authentication authentication
    ) {
        cephService.deleteImage(pool, image, authentication.getName());
        return ApiResponse.ok(null, "RBD image deleted");
    }
}
