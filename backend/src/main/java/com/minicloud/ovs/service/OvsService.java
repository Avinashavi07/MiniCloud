package com.minicloud.ovs.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.infrastructure.ovs.CreateOvsBridgeRequest;
import com.minicloud.infrastructure.ovs.OvsBridgeInfo;
import com.minicloud.infrastructure.ovs.OvsCapabilities;
import com.minicloud.infrastructure.ovs.OvsProvider;
import com.minicloud.ovs.dto.CreateOvsBridgeRequestDto;
import com.minicloud.ovs.dto.OvsListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OvsService {

    private static final Logger log = LoggerFactory.getLogger(OvsService.class);

    private final OvsProvider ovsProvider;
    private final AuditService auditService;

    public OvsService(OvsProvider ovsProvider, AuditService auditService) {
        this.ovsProvider = ovsProvider;
        this.auditService = auditService;
    }

    public OvsListResponse list() {
        OvsCapabilities capabilities = ovsProvider.capabilities();
        String note = capabilities.connected()
                ? "Bridges come from Open vSwitch (ovs-vsctl). libvirt NAT networks stay on /networks."
                : capabilities.detail();
        log.info("OVS inventory requested: status={} bridges={}", capabilities.status(),
                capabilities.connected() ? ovsProvider.listBridges().size() : 0);
        return new OvsListResponse(
                capabilities,
                note,
                capabilities.connected() ? ovsProvider.listBridges() : java.util.List.of());
    }

    public OvsBridgeInfo get(String name) {
        log.info("Reading OVS bridge {}", name);
        return ovsProvider.getBridge(name);
    }

    public OvsBridgeInfo create(CreateOvsBridgeRequestDto request, String username) {
        log.info("OVS bridge creation requested name={}", request.getName());
        try {
            OvsBridgeInfo created = ovsProvider.createBridge(new CreateOvsBridgeRequest(request.getName()));
            auditService.record(username, "OVS_BRIDGE_CREATED", "OVS", created.name(), "SUCCESS",
                    "Created OVS bridge " + created.name());
            return created;
        } catch (RuntimeException ex) {
            auditService.record(username, "OVS_BRIDGE_CREATED", "OVS", request.getName(), "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public void delete(String name, String username) {
        log.info("OVS bridge deletion requested for {}", name);
        try {
            ovsProvider.deleteBridge(name);
            auditService.record(username, "OVS_BRIDGE_DELETED", "OVS", name, "SUCCESS",
                    "Deleted OVS bridge " + name);
        } catch (RuntimeException ex) {
            auditService.record(username, "OVS_BRIDGE_DELETED", "OVS", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }
}
