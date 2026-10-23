package com.minicloud.console.dto;

import com.minicloud.infrastructure.console.ConsoleConnectionInfo;

public record ConsoleSessionResponse(
        ConsoleConnectionInfo console,
        String ticket,
        String websocketPath,
        long expiresAtEpochMs
) {
}
