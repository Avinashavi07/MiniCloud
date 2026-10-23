package com.minicloud.infrastructure.network;

public record ConnectedVmInfo(
        String vmName,
        String interfaceName,
        String mac,
        String ip
) {
}
