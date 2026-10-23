package com.minicloud.vm.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.hypervisor.dto.VmListResponse;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import com.minicloud.vm.dto.AttachDiskRequestDto;
import com.minicloud.vm.dto.AttachRbdDiskRequestDto;
import com.minicloud.vm.dto.AttachInterfaceRequestDto;
import com.minicloud.vm.dto.AttachOvsInterfaceRequestDto;
import com.minicloud.vm.dto.CreateVmRequestDto;
import com.minicloud.vm.dto.VmCatalogResponse;
import com.minicloud.vm.service.VmService;
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
@RequestMapping("/api/v1/vms")
public class VmController {

    private final VmService vmService;

    public VmController(VmService vmService) {
        this.vmService = vmService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<VmListResponse> list() {
        VmListResponse data = vmService.list();
        return ApiResponse.ok(data, data.note());
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<VirtualMachineInfo> get(@PathVariable String name) {
        return ApiResponse.ok(vmService.get(name), "Domain read from libvirt");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> create(@Valid @RequestBody CreateVmRequestDto request, Authentication authentication) {
        return ApiResponse.ok(vmService.create(request, authentication.getName()), "VM created in libvirt");
    }

    @PostMapping("/{name}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> start(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(vmService.start(name, authentication.getName()), "VM started successfully");
    }

    @PostMapping("/{name}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> stop(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(vmService.stop(name, authentication.getName()), "VM stopped successfully");
    }

    @PostMapping("/{name}/restart")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> restart(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(vmService.restart(name, authentication.getName()), "VM restart requested");
    }

    @PostMapping("/{name}/shutdown")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> shutdown(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(vmService.shutdown(name, authentication.getName()), "VM shutdown requested");
    }

    @PostMapping("/{name}/interfaces")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> attachInterface(
            @PathVariable String name,
            @Valid @RequestBody AttachInterfaceRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                vmService.attachInterface(name, request, authentication.getName()),
                "Interface attached in libvirt"
        );
    }

    @PostMapping("/{name}/ovs-interfaces")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> attachOvsInterface(
            @PathVariable String name,
            @Valid @RequestBody AttachOvsInterfaceRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                vmService.attachOvsInterface(name, request, authentication.getName()),
                "OVS interface attached in libvirt"
        );
    }

    @DeleteMapping("/{name}/interfaces/{mac}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> detachInterface(
            @PathVariable String name,
            @PathVariable String mac,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                vmService.detachInterface(name, mac, authentication.getName()),
                "Interface detached in libvirt"
        );
    }

    @PostMapping("/{name}/disks")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> attachDisk(
            @PathVariable String name,
            @Valid @RequestBody AttachDiskRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                vmService.attachDisk(name, request, authentication.getName()),
                "Disk attached in libvirt"
        );
    }

    @PostMapping("/{name}/rbd-disks")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> attachRbdDisk(
            @PathVariable String name,
            @Valid @RequestBody AttachRbdDiskRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                vmService.attachRbdDisk(name, request, authentication.getName()),
                "RBD disk attached in libvirt"
        );
    }

    @DeleteMapping("/{name}/disks/{target}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<VirtualMachineInfo> detachDisk(
            @PathVariable String name,
            @PathVariable String target,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                vmService.detachDisk(name, target, authentication.getName()),
                "Disk detached in libvirt"
        );
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> delete(@PathVariable String name, Authentication authentication) {
        vmService.delete(name, authentication.getName());
        return ApiResponse.ok(null, "VM deleted from libvirt");
    }
}
