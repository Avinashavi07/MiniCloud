package com.minicloud.infrastructure.network;

import java.util.List;

/**
 * Live libvirt virtual network. Not a PostgreSQL row.
 */
public record VirtualNetworkInfo(
        String name,
        String uuid,
        String state,
        boolean active,
        boolean persistent,
        boolean autostart,
        String bridge,
        String forwardMode,
        String cidr,
        String gateway,
        String dhcpStart,
        String dhcpEnd,
        List<ConnectedVmInfo> connectedVms
) {
    public VirtualNetworkInfo {
        connectedVms = connectedVms == null ? List.of() : List.copyOf(connectedVms);
    }

    public VirtualNetworkInfo withConnectedVms(List<ConnectedVmInfo> vms) {
        return new VirtualNetworkInfo(
                name, uuid, state, active, persistent, autostart,
                bridge, forwardMode, cidr, gateway, dhcpStart, dhcpEnd, vms
        );
    }
}
