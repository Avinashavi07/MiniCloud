package com.minicloud.infrastructure.network;

public record CreateNetworkRequest(
        String name,
        String cidr,
        String gateway,
        String dhcpStart,
        String dhcpEnd,
        boolean autostart
) {
}
