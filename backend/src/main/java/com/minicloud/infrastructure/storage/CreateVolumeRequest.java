package com.minicloud.infrastructure.storage;

public record CreateVolumeRequest(String pool, String name, long sizeGb, String format) {
}
