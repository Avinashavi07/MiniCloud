package com.minicloud.infrastructure.ovs;

/**
 * Open vSwitch boundary. Phase 7 talks to ovs-vsctl on the host.
 * OVN and VXLAN overlays stay for later phases.
 */
public interface OvsProvider {

    OvsCapabilities capabilities();

    java.util.List<OvsBridgeInfo> listBridges();

    OvsBridgeInfo getBridge(String name);

    OvsBridgeInfo createBridge(CreateOvsBridgeRequest request);

    void deleteBridge(String name);
}
