package com.minicloud.infrastructure.ceph;

import com.minicloud.common.exception.InvalidCephConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CephNamesTest {

    @Test
    void acceptsManagedPoolNames() {
        assertThat(CephNames.requireManagedPool("mc-rbd")).isEqualTo("mc-rbd");
        assertThat(CephNames.looksManaged("mc-rbd")).isTrue();
    }

    @Test
    void rejectsUnmanagedPoolCreates() {
        assertThatThrownBy(() -> CephNames.requireManagedPool("rbd"))
                .isInstanceOf(InvalidCephConfigurationException.class);
    }

    @Test
    void rejectsReservedPools() {
        assertThatThrownBy(() -> CephNames.requireSafe(".mgr", "pool"))
                .isInstanceOf(InvalidCephConfigurationException.class);
    }
}
