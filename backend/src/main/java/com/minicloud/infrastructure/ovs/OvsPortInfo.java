package com.minicloud.infrastructure.ovs;

import java.util.List;

/**
 * A port on an OVS bridge (vnet TAP, internal, patch, …).
 */
public record OvsPortInfo(
        String name,
        String uuid,
        String type,
        List<String> interfaces,
        String ofport
) {
    public OvsPortInfo {
        interfaces = interfaces == null ? List.of() : List.copyOf(interfaces);
    }
}
