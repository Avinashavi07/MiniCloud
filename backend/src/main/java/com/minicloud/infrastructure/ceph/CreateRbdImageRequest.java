package com.minicloud.infrastructure.ceph;

public record CreateRbdImageRequest(String pool, String name, long sizeGb) {
}
