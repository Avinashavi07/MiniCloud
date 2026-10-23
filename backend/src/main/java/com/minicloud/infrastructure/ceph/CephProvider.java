package com.minicloud.infrastructure.ceph;

import java.util.List;

/**
 * Ceph / RBD boundary. Phase 8 talks to {@code ceph} and {@code rbd}.
 * Directory pools stay on StorageProvider.
 */
public interface CephProvider {

    CephCapabilities capabilities();

    CephClusterStatus status();

    List<CephPoolInfo> listPools();

    CephPoolInfo getPool(String name);

    CephPoolInfo createPool(CreateCephPoolRequest request);

    void deletePool(String name);

    List<RbdImageInfo> listImages(String pool);

    RbdImageInfo getImage(String pool, String image);

    RbdImageInfo createImage(CreateRbdImageRequest request);

    void deleteImage(String pool, String image);
}
