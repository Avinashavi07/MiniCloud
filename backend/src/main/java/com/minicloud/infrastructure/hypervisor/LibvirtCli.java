package com.minicloud.infrastructure.hypervisor;

import com.minicloud.common.exception.NetworkOperationException;
import com.minicloud.common.exception.VmOperationException;
import com.minicloud.config.MiniCloudProperties;
import com.minicloud.infrastructure.process.CommandResult;
import com.minicloud.infrastructure.process.ControlledProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a fixed virsh argument list against the configured libvirt URI.
 * Domain, pool, network, and volume names are validated before they are appended.
 */
@Component
public class LibvirtCli {

    private static final Logger log = LoggerFactory.getLogger(LibvirtCli.class);

    private final MiniCloudProperties properties;

    public LibvirtCli(MiniCloudProperties properties) {
        this.properties = properties;
    }

    public String uri() {
        return properties.getHypervisor().getUri();
    }

    public CommandResult version() {
        return run(5, "version");
    }

    public CommandResult nodeinfo() {
        return run(5, "nodeinfo");
    }

    public CommandResult listAll() {
        return run(8, "list", "--all");
    }

    public CommandResult dominfo(String name) {
        return run(8, "dominfo", LibvirtNames.requireSafe(name));
    }

    public CommandResult blkList(String name) {
        return run(8, "domblklist", LibvirtNames.requireSafe(name), "--details");
    }

    public CommandResult ifList(String name) {
        return run(8, "domiflist", LibvirtNames.requireSafe(name));
    }

    public CommandResult ifAddr(String name) {
        return run(8, "domifaddr", LibvirtNames.requireSafe(name), "--source", "lease");
    }

    public CommandResult dumpXml(String name) {
        return run(8, "dumpxml", LibvirtNames.requireSafe(name));
    }

    public CommandResult vncDisplay(String name) {
        return run(8, "vncdisplay", LibvirtNames.requireSafe(name));
    }

    public CommandResult start(String name) {
        return run(30, "start", LibvirtNames.requireSafe(name));
    }

    public CommandResult destroy(String name) {
        return run(15, "destroy", LibvirtNames.requireSafe(name));
    }

    public CommandResult shutdown(String name) {
        return run(20, "shutdown", LibvirtNames.requireSafe(name));
    }

    public CommandResult reboot(String name) {
        return run(20, "reboot", LibvirtNames.requireSafe(name));
    }

    public CommandResult undefine(String name, boolean removeStorage) {
        if (removeStorage) {
            return run(30, "undefine", LibvirtNames.requireSafe(name), "--remove-all-storage");
        }
        return run(15, "undefine", LibvirtNames.requireSafe(name));
    }

    public CommandResult netList() {
        return run(8, "net-list", "--all");
    }

    public CommandResult netInfo(String name) {
        return run(8, "net-info", LibvirtNames.requireSafe(name, "network"));
    }

    public CommandResult netDumpXml(String name) {
        return run(8, "net-dumpxml", LibvirtNames.requireSafe(name, "network"));
    }

    public CommandResult netDhcpLeases(String name) {
        return run(8, "net-dhcp-leases", LibvirtNames.requireSafe(name, "network"));
    }

    public CommandResult netStart(String name) {
        return run(20, "net-start", LibvirtNames.requireSafe(name, "network"));
    }

    public CommandResult netDestroy(String name) {
        return run(20, "net-destroy", LibvirtNames.requireSafe(name, "network"));
    }

    public CommandResult netUndefine(String name) {
        return run(15, "net-undefine", LibvirtNames.requireSafe(name, "network"));
    }

    public CommandResult netAutostart(String name, boolean enabled) {
        if (enabled) {
            return run(8, "net-autostart", LibvirtNames.requireSafe(name, "network"));
        }
        return run(8, "net-autostart", LibvirtNames.requireSafe(name, "network"), "--disable");
    }

    public CommandResult netDefine(String xml) {
        Path temp = null;
        try {
            temp = Files.createTempFile("minicloud-network-", ".xml");
            Files.writeString(temp, xml);
            return run(20, "net-define", temp.toString());
        } catch (IOException ex) {
            throw new NetworkOperationException("Could not write temporary network XML: " + ex.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    log.debug("Could not delete temporary network XML {}", temp);
                }
            }
        }
    }

    public CommandResult attachInterface(String domain, String network, String model, boolean live) {
        if (live) {
            return run(20,
                    "attach-interface", LibvirtNames.requireSafe(domain, "domain"),
                    "--type", "network",
                    "--source", LibvirtNames.requireSafe(network, "network"),
                    "--model", LibvirtNames.requireNicModel(model),
                    "--config",
                    "--live");
        }
        return run(20,
                "attach-interface", LibvirtNames.requireSafe(domain, "domain"),
                "--type", "network",
                "--source", LibvirtNames.requireSafe(network, "network"),
                "--model", LibvirtNames.requireNicModel(model),
                "--config");
    }

    public CommandResult attachOvsInterface(String domain, String bridge, String model, boolean live) {
        String xml = """
                <interface type='bridge'>
                  <source bridge='%s'/>
                  <virtualport type='openvswitch'/>
                  <model type='%s'/>
                </interface>
                """.formatted(
                escapeXml(com.minicloud.infrastructure.ovs.OvsNames.requireSafe(bridge)),
                escapeXml(LibvirtNames.requireNicModel(model)));
        return attachDevice(domain, xml, live);
    }

    public CommandResult attachDevice(String domain, String xml, boolean live) {
        // OVS path historically passes --live and --config together for running domains.
        return attachDevice(domain, xml, live, true);
    }

    public CommandResult attachDevice(String domain, String xml, boolean live, boolean config) {
        Path temp = null;
        try {
            temp = Files.createTempFile("minicloud-device-", ".xml");
            Files.writeString(temp, xml);
            List<String> args = new ArrayList<>();
            args.add("attach-device");
            args.add(LibvirtNames.requireSafe(domain, "domain"));
            args.add(temp.toString());
            if (config) {
                args.add("--config");
            }
            if (live) {
                args.add("--live");
            }
            return run(20, args.toArray(String[]::new));
        } catch (IOException ex) {
            throw new VmOperationException("Could not write temporary device XML: " + ex.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    log.debug("Could not delete temporary device XML {}", temp);
                }
            }
        }
    }

    public CommandResult detachInterface(String domain, String mac, String type, boolean live) {
        String safeType = type == null || type.isBlank() ? "network" : type.toLowerCase();
        if (!safeType.matches("^(network|bridge)$")) {
            safeType = "network";
        }
        if (live) {
            return run(20,
                    "detach-interface", LibvirtNames.requireSafe(domain, "domain"),
                    "--type", safeType,
                    "--mac", LibvirtNames.requireMac(mac),
                    "--config",
                    "--live");
        }
        return run(20,
                "detach-interface", LibvirtNames.requireSafe(domain, "domain"),
                "--type", safeType,
                "--mac", LibvirtNames.requireMac(mac),
                "--config");
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public CommandResult poolList() {
        return run(8, "pool-list", "--all");
    }

    public CommandResult poolInfo(String pool) {
        return run(8, "pool-info", LibvirtNames.requireSafe(pool, "pool"));
    }

    public CommandResult poolDumpXml(String pool) {
        return run(8, "pool-dumpxml", LibvirtNames.requireSafe(pool, "pool"));
    }

    public CommandResult poolDestroy(String pool) {
        return run(20, "pool-destroy", LibvirtNames.requireSafe(pool, "pool"));
    }

    public CommandResult poolUndefine(String pool) {
        return run(15, "pool-undefine", LibvirtNames.requireSafe(pool, "pool"));
    }

    public CommandResult poolDefineAs(String pool, String target) {
        return run(15, "pool-define-as", LibvirtNames.requireSafe(pool), "dir", "--target", target);
    }

    public CommandResult poolBuild(String pool) {
        return run(15, "pool-build", LibvirtNames.requireSafe(pool));
    }

    public CommandResult poolStart(String pool) {
        return run(15, "pool-start", LibvirtNames.requireSafe(pool));
    }

    public CommandResult poolAutostart(String pool) {
        return poolAutostart(pool, true);
    }

    public CommandResult poolAutostart(String pool, boolean enabled) {
        if (enabled) {
            return run(8, "pool-autostart", LibvirtNames.requireSafe(pool, "pool"));
        }
        return run(8, "pool-autostart", LibvirtNames.requireSafe(pool, "pool"), "--disable");
    }

    public CommandResult volList(String pool) {
        return run(8, "vol-list", "--details", LibvirtNames.requireSafe(pool, "pool"));
    }

    public CommandResult volCreateAs(String pool, String volume, long diskGb) {
        return volCreateAs(pool, volume, diskGb, "qcow2");
    }

    public CommandResult volCreateAs(String pool, String volume, long diskGb, String format) {
        if (diskGb < 1 || diskGb > 2048) {
            throw new VmOperationException("Disk size must be between 1 and 2048 GB");
        }
        String safeFormat = format != null && format.equalsIgnoreCase("raw") ? "raw" : "qcow2";
        return run(30, "vol-create-as", LibvirtNames.requireSafe(pool, "pool"), LibvirtNames.requireSafe(volume, "volume"),
                diskGb + "G", "--format", safeFormat);
    }

    public CommandResult attachDisk(String domain, String path, String target, String format, boolean live) {
        String safePath = com.minicloud.infrastructure.storage.StoragePaths.requireSafeVolumePath(path);
        String safeTarget = LibvirtNames.requireDiskTarget(target);
        String safeFormat = format != null && format.equalsIgnoreCase("raw") ? "raw" : "qcow2";
        if (live) {
            return run(20, "attach-disk", LibvirtNames.requireSafe(domain, "domain"),
                    safePath, safeTarget,
                    "--driver", "qemu",
                    "--subdriver", safeFormat,
                    "--targetbus", "virtio",
                    "--live");
        }
        return run(20, "attach-disk", LibvirtNames.requireSafe(domain, "domain"),
                safePath, safeTarget,
                "--driver", "qemu",
                "--subdriver", safeFormat,
                "--targetbus", "virtio",
                "--config");
    }

    public CommandResult secretList() {
        return run(8, "secret-list");
    }

    public CommandResult secretDefine(String xml) {
        Path temp = null;
        try {
            temp = Files.createTempFile("minicloud-secret-", ".xml");
            Files.writeString(temp, xml);
            return run(20, "secret-define", temp.toString());
        } catch (IOException ex) {
            throw new VmOperationException("Could not write temporary secret XML: " + ex.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    log.debug("Could not delete temporary secret XML {}", temp);
                }
            }
        }
    }

    public CommandResult secretSetValue(String uuid, String base64Key) {
        Path temp = null;
        try {
            temp = Files.createTempFile("minicloud-secret-val-", ".b64");
            Files.writeString(temp, base64Key);
            return run(20, "secret-set-value", uuid, "--file", temp.toString());
        } catch (IOException ex) {
            throw new VmOperationException("Could not write temporary secret value: " + ex.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    log.debug("Could not delete temporary secret value {}", temp);
                }
            }
        }
    }

    public CommandResult detachDisk(String domain, String target, boolean live) {
        String safeTarget = LibvirtNames.requireDiskTarget(target);
        if (live) {
            return run(20, "detach-disk", LibvirtNames.requireSafe(domain, "domain"),
                    safeTarget, "--live");
        }
        return run(20, "detach-disk", LibvirtNames.requireSafe(domain, "domain"),
                safeTarget, "--config");
    }

    public CommandResult volDelete(String pool, String volume) {
        return run(20, "vol-delete", "--pool", LibvirtNames.requireSafe(pool), LibvirtNames.requireSafe(volume));
    }

    public CommandResult volPath(String pool, String volume) {
        return run(8, "vol-path", "--pool", LibvirtNames.requireSafe(pool), LibvirtNames.requireSafe(volume));
    }

    public CommandResult define(String xml) {
        Path temp = null;
        try {
            temp = Files.createTempFile("minicloud-domain-", ".xml");
            Files.writeString(temp, xml);
            return run(20, "define", temp.toString());
        } catch (IOException ex) {
            throw new VmOperationException("Could not write temporary domain XML: " + ex.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    log.debug("Could not delete temporary domain XML {}", temp);
                }
            }
        }
    }

    private CommandResult run(long timeoutSeconds, String... virshArgs) {
        List<String> command = new ArrayList<>();
        command.add("virsh");
        command.add("--connect");
        command.add(uri());
        command.addAll(List.of(virshArgs));
        log.debug("libvirt command: {}", String.join(" ", command));
        return ControlledProcess.run(timeoutSeconds, command);
    }
}
