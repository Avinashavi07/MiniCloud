package com.minicloud.network.dto;

import com.minicloud.infrastructure.network.VirtualNetworkInfo;

import java.util.List;

public record NetworkListResponse(
        boolean listingAvailable,
        String note,
        List<VirtualNetworkInfo> networks
) {
}
