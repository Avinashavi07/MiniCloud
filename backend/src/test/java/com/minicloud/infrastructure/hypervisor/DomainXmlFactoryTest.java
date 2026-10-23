package com.minicloud.infrastructure.hypervisor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainXmlFactoryTest {

    @Test
    void buildsKvmDomainWithIsoAndEscapesName() {
        String xml = DomainXmlFactory.kvmDomain(
                "web-01",
                2,
                4096,
                "/var/lib/libvirt/images/web-01.qcow2",
                "/var/lib/libvirt/images/ubuntu.iso",
                "default",
                "/usr/bin/qemu-system-x86_64"
        );

        assertThat(xml).contains("<domain type='kvm'>");
        assertThat(xml).contains("<name>web-01</name>");
        assertThat(xml).contains("<vcpu placement='static'>2</vcpu>");
        assertThat(xml).contains("<memory unit='MiB'>4096</memory>");
        assertThat(xml).contains("source file='/var/lib/libvirt/images/web-01.qcow2'");
        assertThat(xml).contains("<boot dev='cdrom'/>");
        assertThat(xml).contains("<source network='default'/>");
        assertThat(xml).contains("<controller type='pci' index='1' model='pcie-root-port'/>");
        assertThat(xml).contains("<controller type='pci' index='14' model='pcie-root-port'/>");
        assertThat(xml).doesNotContain("<script>");
    }

    @Test
    void escapesXmlMetacharacters() {
        assertThat(DomainXmlFactory.escape("a<b&c>'\"")).isEqualTo("a&lt;b&amp;c&gt;&apos;&quot;");
    }
}
