package com.minicloud.infrastructure.storage;

import com.minicloud.common.exception.InvalidStorageConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePathsTest {

    @Test
    void acceptsImagesTree() {
        assertThat(StoragePaths.requireSafeDirectory("/var/lib/libvirt/images")).isEqualTo("/var/lib/libvirt/images");
        assertThat(StoragePaths.requireSafeDirectory("/var/lib/libvirt/images/minicloud-pool-01"))
                .isEqualTo("/var/lib/libvirt/images/minicloud-pool-01");
        assertThat(StoragePaths.requireSafeVolumePath("/var/lib/libvirt/images/ubuntu-test-02.qcow2"))
                .endsWith("ubuntu-test-02.qcow2");
    }

    @Test
    void rejectsUnsafePaths() {
        assertThatThrownBy(() -> StoragePaths.requireSafeDirectory("/etc/passwd"))
                .isInstanceOf(InvalidStorageConfigurationException.class);
        assertThatThrownBy(() -> StoragePaths.requireSafeDirectory("/var/lib/libvirt/images/../qemu"))
                .isInstanceOf(InvalidStorageConfigurationException.class);
        assertThatThrownBy(() -> StoragePaths.requireFormat("vmdk"))
                .isInstanceOf(InvalidStorageConfigurationException.class);
    }
}
