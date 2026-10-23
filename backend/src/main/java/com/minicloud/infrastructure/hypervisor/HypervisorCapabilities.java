package com.minicloud.infrastructure.hypervisor;

public record HypervisorCapabilities(
        String providerType,
        String uri,
        boolean kvmAvailable,
        boolean connected,
        String status,
        String detail,
        boolean lifecycleImplemented,
        String libvirtVersion,
        String hypervisorVersion,
        LibvirtNodeInfo node
) {
    public static HypervisorCapabilities of(
            String providerType,
            String uri,
            boolean kvmAvailable,
            boolean connected,
            String status,
            String detail,
            boolean lifecycleImplemented
    ) {
        return new HypervisorCapabilities(
                providerType, uri, kvmAvailable, connected, status, detail, lifecycleImplemented,
                null, null, null
        );
    }
}
