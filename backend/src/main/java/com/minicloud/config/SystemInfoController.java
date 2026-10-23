package com.minicloud.config;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemInfoController {

    private final MiniCloudProperties properties;
    private final HypervisorProvider hypervisorProvider;

    public SystemInfoController(MiniCloudProperties properties, HypervisorProvider hypervisorProvider) {
        this.properties = properties;
        this.hypervisorProvider = hypervisorProvider;
    }

    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<Map<String, Object>> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", "MiniCloud");
        data.put("phase", properties.getPhase());
        data.put("phaseName", "Ceph / RBD distributed storage");
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("hypervisor", hypervisorProvider.capabilities());
        data.put("implemented", new String[]{
                "authentication", "host-discovery", "libvirt-connect", "vm-list", "vm-create",
                "vm-start", "vm-stop", "vm-restart", "vm-shutdown", "vm-delete",
                "network-list", "network-create", "network-lifecycle", "vm-attach-interface", "vm-detach-interface",
                "storage-list", "storage-pool-create", "storage-volume-create", "vm-attach-disk", "vm-detach-disk",
                "console-list", "console-vnc", "console-websocket-bridge",
                "ovs-discover", "ovs-bridge-create", "ovs-bridge-delete", "vm-attach-ovs-interface",
                "ceph-discover", "ceph-pool-create", "rbd-image-create", "vm-attach-rbd"
        });
        data.put("notImplemented", new String[]{"vxlan", "ovn", "lvm-pools", "iscsi", "guacamole", "rdp", "spice", "sriov", "multi-node"});
        return ApiResponse.ok(data);
    }
}
