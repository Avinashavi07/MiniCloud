package com.minicloud.infrastructure.ovs;

import java.util.List;

/**
 * Live view of an OVS bridge. Not a PostgreSQL row.
 */
public record OvsBridgeInfo(
        String name,
        String uuid,
        boolean managedByMiniCloud,
        List<OvsPortInfo> ports,
        String datapathId,
        String detail
) {
    public OvsBridgeInfo {
        ports = ports == null ? List.of() : List.copyOf(ports);
    }
}
