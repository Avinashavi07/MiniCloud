package com.minicloud.infrastructure.network;

import com.minicloud.common.exception.InvalidNetworkConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Ipv4NetworksTest {

    @Test
    void validatesTypicalNatNetwork() {
        Ipv4Networks.validateCreate("192.168.100.0/24", "192.168.100.1", "192.168.100.10", "192.168.100.200");
        Ipv4Networks.Block block = Ipv4Networks.parseCidr("192.168.100.0/24");
        assertThat(block.netmaskString()).isEqualTo("255.255.255.0");
        assertThat(block.contains(Ipv4Networks.parseIp("192.168.100.1"))).isTrue();
    }

    @Test
    void rejectsOverlapAndPublicRanges() {
        Ipv4Networks.Block a = Ipv4Networks.parseCidr("192.168.122.0/24");
        Ipv4Networks.Block b = Ipv4Networks.parseCidr("192.168.122.128/25");
        Ipv4Networks.Block c = Ipv4Networks.parseCidr("192.168.100.0/24");
        assertThat(a.overlaps(b)).isTrue();
        assertThat(a.overlaps(c)).isFalse();
        assertThatThrownBy(() -> Ipv4Networks.validateCreate("8.8.8.0/24", "8.8.8.1", "8.8.8.10", "8.8.8.20"))
                .isInstanceOf(InvalidNetworkConfigurationException.class);
        assertThatThrownBy(() -> Ipv4Networks.validateCreate("192.168.100.0/24", "192.168.101.1", "192.168.100.10", "192.168.100.20"))
                .isInstanceOf(InvalidNetworkConfigurationException.class);
    }
}
