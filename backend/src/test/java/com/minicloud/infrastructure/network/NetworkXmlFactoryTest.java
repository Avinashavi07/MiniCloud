package com.minicloud.infrastructure.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkXmlFactoryTest {

    @Test
    void buildsNatXmlAndParsesItBack() {
        String xml = NetworkXmlFactory.natNetwork(
                "minicloud-net-01", "192.168.100.1", "255.255.255.0", "192.168.100.10", "192.168.100.200");

        assertThat(xml).contains("<forward mode='nat'>");
        assertThat(xml).contains("<name>minicloud-net-01</name>");

        NetworkXmlParser.Addressing addressing = NetworkXmlParser.parse(xml);
        assertThat(addressing.name()).isEqualTo("minicloud-net-01");
        assertThat(addressing.forwardMode()).isEqualTo("nat");
        assertThat(addressing.gateway()).isEqualTo("192.168.100.1");
        assertThat(addressing.cidr()).isEqualTo("192.168.100.0/24");
        assertThat(addressing.dhcpStart()).isEqualTo("192.168.100.10");
        assertThat(addressing.dhcpEnd()).isEqualTo("192.168.100.200");
    }
}
