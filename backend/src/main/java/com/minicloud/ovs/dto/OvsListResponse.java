package com.minicloud.ovs.dto;

import com.minicloud.infrastructure.ovs.OvsBridgeInfo;
import com.minicloud.infrastructure.ovs.OvsCapabilities;

import java.util.List;

public record OvsListResponse(
        OvsCapabilities capabilities,
        String note,
        List<OvsBridgeInfo> bridges
) {
}
