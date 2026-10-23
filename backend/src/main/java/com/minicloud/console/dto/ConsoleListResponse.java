package com.minicloud.console.dto;

import com.minicloud.infrastructure.console.ConsoleConnectionInfo;

import java.util.List;

public record ConsoleListResponse(
        boolean listingAvailable,
        String note,
        List<ConsoleConnectionInfo> consoles
) {
}
