package com.minicloud.infrastructure.hypervisor;

/**
 * Builds libvirt domain XML. Values are escaped; callers must already validate names.
 */
public final class DomainXmlFactory {

    private DomainXmlFactory() {
    }

    public static String kvmDomain(
            String name,
            int cpuCores,
            long memoryMb,
            String diskPath,
            String isoPath,
            String network,
            String emulator
    ) {
        StringBuilder xml = new StringBuilder();
        xml.append("<domain type='kvm'>\n");
        xml.append("  <name>").append(escape(name)).append("</name>\n");
        xml.append("  <memory unit='MiB'>").append(memoryMb).append("</memory>\n");
        xml.append("  <currentMemory unit='MiB'>").append(memoryMb).append("</currentMemory>\n");
        xml.append("  <vcpu placement='static'>").append(cpuCores).append("</vcpu>\n");
        xml.append("  <os>\n");
        xml.append("    <type arch='x86_64' machine='q35'>hvm</type>\n");
        if (isoPath != null) {
            xml.append("    <boot dev='cdrom'/>\n");
        }
        xml.append("    <boot dev='hd'/>\n");
        xml.append("  </os>\n");
        xml.append("  <features>\n    <acpi/>\n    <apic/>\n  </features>\n");
        xml.append("  <cpu mode='host-passthrough' check='none'/>\n");
        xml.append("  <clock offset='utc'/>\n");
        xml.append("  <on_poweroff>destroy</on_poweroff>\n");
        xml.append("  <on_reboot>restart</on_reboot>\n");
        xml.append("  <on_crash>destroy</on_crash>\n");
        xml.append("  <devices>\n");
        xml.append("    <emulator>").append(escape(emulator)).append("</emulator>\n");
        xml.append("    <disk type='file' device='disk'>\n");
        xml.append("      <driver name='qemu' type='qcow2'/>\n");
        xml.append("      <source file='").append(escape(diskPath)).append("'/>\n");
        xml.append("      <target dev='vda' bus='virtio'/>\n");
        xml.append("    </disk>\n");
        if (isoPath != null) {
            xml.append("    <disk type='file' device='cdrom'>\n");
            xml.append("      <driver name='qemu' type='raw'/>\n");
            xml.append("      <source file='").append(escape(isoPath)).append("'/>\n");
            xml.append("      <target dev='sda' bus='sata'/>\n");
            xml.append("      <readonly/>\n");
            xml.append("    </disk>\n");
        }
        xml.append("    <interface type='network'>\n");
        xml.append("      <source network='").append(escape(network)).append("'/>\n");
        xml.append("      <model type='virtio'/>\n");
        xml.append("    </interface>\n");
        // Spare PCIe root ports so later attach-disk / attach-interface can hotplug.
        for (int index = 1; index <= 14; index++) {
            xml.append("    <controller type='pci' index='").append(index).append("' model='pcie-root-port'/>\n");
        }
        xml.append("    <graphics type='vnc' port='-1' listen='127.0.0.1'>\n");
        xml.append("      <listen type='address' address='127.0.0.1'/>\n");
        xml.append("    </graphics>\n");
        xml.append("    <console type='pty'/>\n");
        xml.append("    <video>\n      <model type='virtio'/>\n    </video>\n");
        xml.append("    <memballoon model='virtio'/>\n");
        xml.append("  </devices>\n");
        xml.append("</domain>\n");
        return xml.toString();
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
