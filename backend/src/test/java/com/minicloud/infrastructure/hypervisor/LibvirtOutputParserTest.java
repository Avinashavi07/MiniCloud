package com.minicloud.infrastructure.hypervisor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LibvirtOutputParserTest {

    @Test
    void parseListIncludesShutOffDomains() {
        String stdout = """
                Id   Name        State
                ---------------------------
                 1   web-01      running
                 -   db-01       shut off
                """;

        List<VirtualMachineInfo> vms = LibvirtOutputParser.parseList(stdout);

        assertThat(vms).hasSize(2);
        assertThat(vms.getFirst().name()).isEqualTo("web-01");
        assertThat(vms.getFirst().normalizedState()).isEqualTo("RUNNING");
        assertThat(vms.get(1).name()).isEqualTo("db-01");
        assertThat(vms.get(1).hypervisorId()).isNull();
        assertThat(vms.get(1).normalizedState()).isEqualTo("STOPPED");
    }

    @Test
    void parseListIgnoresUnsafeNames() {
        String stdout = """
                Id   Name        State
                ---------------------------
                 1   good-vm     running
                 2   bad;id      running
                """;

        assertThat(LibvirtOutputParser.parseList(stdout))
                .extracting(VirtualMachineInfo::name)
                .containsExactly("good-vm");
    }

    @Test
    void mergeDominfoReadsCpuAndMemory() {
        VirtualMachineInfo base = VirtualMachineInfo.summary("web-01", "1", "running", "RUNNING");
        String stdout = """
                Id:             1
                Name:           web-01
                UUID:           11111111-1111-1111-1111-111111111111
                OS Type:        hvm
                State:          running
                CPU(s):         2
                Max memory:     4194304 KiB
                Used memory:    2097152 KiB
                Persistent:     yes
                Autostart:      disable
                """;

        VirtualMachineInfo vm = LibvirtOutputParser.mergeDominfo(base, stdout);

        assertThat(vm.uuid()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(vm.cpuCores()).isEqualTo(2);
        assertThat(vm.memoryBytes()).isEqualTo(4194304L * 1024);
        assertThat(vm.memoryUsedBytes()).isEqualTo(2097152L * 1024);
        assertThat(vm.persistent()).isTrue();
        assertThat(vm.autostart()).isFalse();
    }

    @Test
    void parseBlkAndIfList() {
        List<VmDiskInfo> disks = LibvirtOutputParser.parseBlkList("""
                Type   Device   Target   Source
                ------------------------------------------------
                file   disk     vda      /var/lib/libvirt/images/web.qcow2
                file   cdrom    sda      -
                """);
        List<VmNicInfo> nics = LibvirtOutputParser.parseIfList("""
                Interface  Type       Source     Model    MAC
                -------------------------------------------------------
                vnet0      network    default    virtio   52:54:00:aa:bb:cc
                """);
        nics = LibvirtOutputParser.applyAddresses(nics, """
                Name       MAC address          Protocol     Address
                -------------------------------------------------------------------------------
                vnet0      52:54:00:aa:bb:cc    ipv4         192.168.122.80/24
                """);

        assertThat(disks).hasSize(2);
        assertThat(disks.getFirst().target()).isEqualTo("vda");
        assertThat(disks.getFirst().source()).endsWith("web.qcow2");
        assertThat(nics.getFirst().mac()).isEqualTo("52:54:00:aa:bb:cc");
        assertThat(nics.getFirst().ip()).isEqualTo("192.168.122.80/24");
        assertThat(nics.getFirst().hostInterface()).isEqualTo("vnet0");
    }

    @Test
    void parseDhcpLeasesByMacWithoutInventing() {
        var leases = LibvirtOutputParser.parseDhcpLeases("""
                 Expiry Time           MAC address         Protocol   IP address           Hostname
                ------------------------------------------------------------------------------------------------
                 2026-09-25 15:00:00   52:54:00:f6:1c:06   ipv4       192.168.122.80/24    ubuntu
                """);
        assertThat(leases).containsEntry("52:54:00:f6:1c:06", "192.168.122.80/24");
        assertThat(LibvirtOutputParser.parseDhcpLeases("")).isEmpty();
    }

    @Test
    void parseNodeAndVersion() {
        LibvirtNodeInfo node = LibvirtOutputParser.parseNodeinfo("""
                CPU model:           x86_64
                CPU(s):              12
                CPU socket(s):       1
                Core(s) per socket:  6
                Thread(s) per core:  2
                Memory size:         16068888 KiB
                """);
        LibvirtVersionInfo version = LibvirtOutputParser.parseVersion("""
                Compiled against library: libvirt 10.0.0
                Using library: libvirt 10.0.0
                Using API: QEMU 10.0.0
                Running hypervisor: QEMU 8.2.2
                """);

        assertThat(node.cpus()).isEqualTo(12);
        assertThat(node.memoryBytes()).isEqualTo(16068888L * 1024);
        assertThat(version.libvirtVersion()).isEqualTo("libvirt 10.0.0");
        assertThat(version.hypervisorVersion()).isEqualTo("QEMU 8.2.2");
    }

    @Test
    void parsePoolAndNetworks() {
        StoragePoolInfo pool = LibvirtOutputParser.parsePoolInfo("default", """
                Name:           default
                State:          running
                Capacity:       253.22 GiB
                Allocation:     78.65 GiB
                Available:      174.57 GiB
                """);
        var networks = LibvirtOutputParser.parseNetList("""
                Name      State    Autostart   Persistent
                --------------------------------------------
                 default   active   yes         yes
                """);

        assertThat(pool.availableBytes()).isGreaterThan(100L * 1024 * 1024 * 1024);
        assertThat(networks).extracting(NetworkSummary::name).containsExactly("default");
    }

    @Test
    void parsePoolListAndVolumes() {
        var pools = LibvirtOutputParser.parsePoolList("""
                Name      State    Autostart
                -------------------------------
                 default   active   yes
                """);
        var volumes = LibvirtOutputParser.parseVolList("""
                 Name                   Path                                           Type   Capacity    Allocation
                ------------------------------------------------------------------------------------------------------
                 ubuntu-test-02.qcow2   /var/lib/libvirt/images/ubuntu-test-02.qcow2   file   20.00 GiB   200.00 KiB
                """);
        assertThat(pools).extracting(StoragePoolInfo::name).containsExactly("default");
        assertThat(volumes).hasSize(1);
        assertThat(volumes.getFirst().name()).isEqualTo("ubuntu-test-02.qcow2");
        assertThat(volumes.getFirst().path()).endsWith("ubuntu-test-02.qcow2");
        assertThat(volumes.getFirst().capacityBytes()).isEqualTo(Math.round(20.00 * 1024d * 1024d * 1024d));
    }

    @Test
    void parseVncDisplayUsesDisplayNumber() {
        var display = LibvirtOutputParser.parseVncDisplay("127.0.0.1:0\n");
        assertThat(display.host()).isEqualTo("127.0.0.1");
        assertThat(display.display()).isZero();
        assertThat(display.port()).isEqualTo(5900);
    }

    @Test
    void parseVncDisplayAcceptsRawPort() {
        var display = LibvirtOutputParser.parseVncDisplay(":5901");
        assertThat(display.host()).isEqualTo("127.0.0.1");
        assertThat(display.port()).isEqualTo(5901);
        assertThat(display.display()).isEqualTo(1);
    }
}
