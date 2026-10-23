package com.minicloud.infrastructure.ceph;

public record CephCapabilities(
        boolean installed,
        boolean connected,
        String status,
        String detail,
        String cephVersion,
        String clusterFsId
) {
}
