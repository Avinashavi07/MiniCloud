package com.minicloud.infrastructure.hypervisor;

public record VmNicInfo(
        String interfaceName,
        String type,
        String network,
        String model,
        String mac,
        String ip,
        String bridge,
        String hostInterface
) {
    public VmNicInfo withIp(String nextIp) {
        return new VmNicInfo(interfaceName, type, network, model, mac, nextIp, bridge, hostInterface);
    }

    public VmNicInfo withBridge(String nextBridge) {
        return new VmNicInfo(interfaceName, type, network, model, mac, ip, nextBridge, hostInterface);
    }
}
