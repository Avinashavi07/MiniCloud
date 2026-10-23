package com.minicloud.ceph.dto;

import com.minicloud.infrastructure.ceph.CephCapabilities;
import com.minicloud.infrastructure.ceph.CephClusterStatus;
import com.minicloud.infrastructure.ceph.CephPoolInfo;

import java.util.List;

public record CephListResponse(
        CephCapabilities capabilities,
        CephClusterStatus status,
        String note,
        List<CephPoolInfo> pools
) {
    public CephListResponse {
        pools = pools == null ? List.of() : List.copyOf(pools);
    }
}
