package com.minicloud.infrastructure.network;

import java.util.List;

/**
 * Virtual network boundary. Phase 4 talks to libvirt NAT/bridge networks.
 * Later phases can add OVS / OVN providers without changing controllers.
 */
public interface NetworkProvider {

    List<VirtualNetworkInfo> listNetworks();

    VirtualNetworkInfo getNetwork(String name);

    VirtualNetworkInfo createNetwork(CreateNetworkRequest request);

    void startNetwork(String name);

    void stopNetwork(String name);

    void deleteNetwork(String name);

    void setAutostart(String name, boolean enabled);
}
