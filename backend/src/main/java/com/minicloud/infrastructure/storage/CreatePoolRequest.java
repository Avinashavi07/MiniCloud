package com.minicloud.infrastructure.storage;

public record CreatePoolRequest(String name, String path, boolean autostart) {
}
