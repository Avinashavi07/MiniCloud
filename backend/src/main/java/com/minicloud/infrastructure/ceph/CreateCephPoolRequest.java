package com.minicloud.infrastructure.ceph;

public record CreateCephPoolRequest(String name, int pgNum) {
}
