package com.minicloud.infrastructure.ovs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OvsNamesTest {

    @Test
    void acceptsManagedNames() {
        assertThat(OvsNames.requireManagedCreateName("mc-br0")).isEqualTo("mc-br0");
    }

    @Test
    void rejectsNonManagedCreate() {
        assertThatThrownBy(() -> OvsNames.requireManagedCreateName("lab-br0"))
                .hasMessageContaining("mc-*");
    }

    @Test
    void rejectsReserved() {
        assertThatThrownBy(() -> OvsNames.requireSafe("virbr0"))
                .hasMessageContaining("reserved");
    }
}
