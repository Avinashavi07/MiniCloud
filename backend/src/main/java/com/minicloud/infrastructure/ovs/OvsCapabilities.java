package com.minicloud.infrastructure.ovs;

public record OvsCapabilities(
        boolean installed,
        boolean connected,
        String status,
        String detail,
        String ovsVersion,
        String dbVersion
) {
}
