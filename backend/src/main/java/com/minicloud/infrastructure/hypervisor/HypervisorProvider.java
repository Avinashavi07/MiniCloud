package com.minicloud.infrastructure.hypervisor;

import java.util.List;

/**
 * Hypervisor boundary. Phase 3 implements create and lifecycle against libvirt.
 */
public interface HypervisorProvider {

    HypervisorCapabilities capabilities();

    List<VirtualMachineInfo> listVms();

    VirtualMachineInfo getVm(String vmId);

    VirtualMachineInfo createVm(CreateVmRequest request);

    void startVm(String vmId);

    void stopVm(String vmId);

    void restartVm(String vmId);

    void shutdownVm(String vmId);

    void deleteVm(String vmId);

    void attachInterface(String vmId, String network, String model);

    void attachOvsInterface(String vmId, String bridge, String model);

    void detachInterface(String vmId, String mac);

    void attachDisk(String vmId, String path, String target, String format);

    void attachRbdDisk(String vmId, String pool, String image, String target);

    void detachDisk(String vmId, String target);

    List<NetworkSummary> listNetworks();

    StoragePoolInfo storagePool();
}
