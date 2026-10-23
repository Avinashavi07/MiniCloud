package com.minicloud.infrastructure.storage;

import java.util.List;

/**
 * Storage-pool boundary. Phase 5 talks to libvirt directory pools.
 * A later Ceph provider can implement this without changing controllers.
 */
public interface StorageProvider {

    List<VirtualStoragePool> listPools();

    VirtualStoragePool getPool(String name);

    VirtualStoragePool createPool(CreatePoolRequest request);

    void startPool(String name);

    void stopPool(String name);

    void deletePool(String name);

    void setAutostart(String name, boolean enabled);

    StorageVolume createVolume(CreateVolumeRequest request);

    void deleteVolume(String pool, String volume);
}
