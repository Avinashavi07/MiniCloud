package com.minicloud.infrastructure.storage;

import com.minicloud.common.exception.InfrastructureConnectionException;
import com.minicloud.common.exception.InvalidStorageConfigurationException;
import com.minicloud.common.exception.StorageAlreadyExistsException;
import com.minicloud.common.exception.StorageInUseException;
import com.minicloud.common.exception.StorageNotFoundException;
import com.minicloud.common.exception.StorageOperationException;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.hypervisor.LibvirtCli;
import com.minicloud.infrastructure.hypervisor.LibvirtNames;
import com.minicloud.infrastructure.hypervisor.LibvirtOutputParser;
import com.minicloud.infrastructure.hypervisor.StoragePoolInfo;
import com.minicloud.infrastructure.process.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LibvirtStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LibvirtStorageProvider.class);

    private final HostMetricsProvider hostMetricsProvider;
    private final LibvirtCli libvirtCli;

    public LibvirtStorageProvider(HostMetricsProvider hostMetricsProvider, LibvirtCli libvirtCli) {
        this.hostMetricsProvider = hostMetricsProvider;
        this.libvirtCli = libvirtCli;
    }

    @Override
    public List<VirtualStoragePool> listPools() {
        if (!connected()) {
            log.info("Skipping storage listing: libvirt is not connected");
            return List.of();
        }
        CommandResult result = libvirtCli.poolList();
        if (!result.succeeded()) {
            log.warn("virsh pool-list failed: {}", firstNonBlank(result.stderr(), result.stdout()));
            return List.of();
        }
        List<VirtualStoragePool> pools = new ArrayList<>();
        for (StoragePoolInfo summary : LibvirtOutputParser.parsePoolList(result.stdout())) {
            pools.add(describe(summary.name(), true));
        }
        log.info("libvirt reported {} storage pool(s)", pools.size());
        return pools;
    }

    @Override
    public VirtualStoragePool getPool(String name) {
        requireConnected();
        LibvirtNames.requireSafe(name, "pool");
        return describe(name, true);
    }

    @Override
    public VirtualStoragePool createPool(CreatePoolRequest request) {
        requireConnected();
        String name = LibvirtNames.requireSafe(request.name(), "pool");
        String path = StoragePaths.requireSafeDirectory(request.path());
        if (exists(name)) {
            throw new StorageAlreadyExistsException("pool", name);
        }
        for (VirtualStoragePool existing : listPools()) {
            if (path.equals(existing.path())) {
                throw new InvalidStorageConfigurationException(
                        "Path " + path + " is already used by pool '" + existing.name() + "'");
            }
        }
        log.info("Defining libvirt directory pool {} at {}", name, path);
        CommandResult define = libvirtCli.poolDefineAs(name, path);
        if (!define.succeeded()) {
            throw new StorageOperationException("libvirt pool-define-as failed: " + firstNonBlank(define.stderr(), define.stdout()));
        }
        CommandResult build = libvirtCli.poolBuild(name);
        if (!build.succeeded() && !containsIgnoreCase(build.stderr(), "already exists")) {
            throw new StorageOperationException("pool-build failed: " + firstNonBlank(build.stderr(), build.stdout()));
        }
        CommandResult start = libvirtCli.poolStart(name);
        if (!start.succeeded()) {
            throw new StorageOperationException("Pool was defined but start failed: " + firstNonBlank(start.stderr(), start.stdout()));
        }
        if (request.autostart()) {
            CommandResult autostart = libvirtCli.poolAutostart(name, true);
            if (!autostart.succeeded()) {
                log.warn("Pool {} started but autostart failed: {}", name, firstNonBlank(autostart.stderr(), autostart.stdout()));
            }
        }
        log.info("Storage pool created in libvirt: {}", name);
        return describe(name, true);
    }

    @Override
    public void startPool(String name) {
        requireConnected();
        String pool = LibvirtNames.requireSafe(name, "pool");
        requireExisting(pool);
        log.info("Starting libvirt pool {}", pool);
        CommandResult result = libvirtCli.poolStart(pool);
        if (!result.succeeded() && !containsIgnoreCase(result.stderr(), "already active")) {
            throw new StorageOperationException("Failed to start pool: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    @Override
    public void stopPool(String name) {
        requireConnected();
        String pool = LibvirtNames.requireSafe(name, "pool");
        requireExisting(pool);
        log.info("Stopping libvirt pool {}", pool);
        CommandResult result = libvirtCli.poolDestroy(pool);
        if (!result.succeeded()
                && !containsIgnoreCase(result.stderr(), "not active")
                && !containsIgnoreCase(result.stderr(), "is not active")) {
            throw new StorageOperationException("Failed to stop pool: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    @Override
    public void deletePool(String name) {
        requireConnected();
        String pool = LibvirtNames.requireSafe(name, "pool");
        if ("default".equals(pool)) {
            throw new StorageInUseException("The libvirt default pool holds VM disks. MiniCloud will not undefine it.");
        }
        VirtualStoragePool current = describe(pool, true);
        if (current.active()) {
            throw new StorageInUseException("libvirt will not undefine an active pool. Stop '" + pool + "' first.");
        }
        if (!current.volumes().isEmpty()) {
            throw new StorageInUseException("Pool '" + pool + "' still has volumes. Delete them first.");
        }
        log.info("Undefining libvirt pool {}", pool);
        CommandResult result = libvirtCli.poolUndefine(pool);
        if (!result.succeeded()) {
            throw new StorageOperationException("Failed to delete pool: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    @Override
    public void setAutostart(String name, boolean enabled) {
        requireConnected();
        String pool = LibvirtNames.requireSafe(name, "pool");
        requireExisting(pool);
        CommandResult result = libvirtCli.poolAutostart(pool, enabled);
        if (!result.succeeded()) {
            throw new StorageOperationException("Failed to change pool autostart: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    @Override
    public StorageVolume createVolume(CreateVolumeRequest request) {
        requireConnected();
        String pool = LibvirtNames.requireSafe(request.pool(), "pool");
        String volume = LibvirtNames.requireSafe(request.name(), "volume");
        String format = StoragePaths.requireFormat(request.format());
        VirtualStoragePool current = describe(pool, true);
        if (!current.active()) {
            throw new StorageOperationException("Pool '" + pool + "' is not active. Start it before creating a volume.");
        }
        if (current.volumes().stream().anyMatch(item -> item.name().equals(volume))) {
            throw new StorageAlreadyExistsException("volume", volume);
        }
        long needed = request.sizeGb() * 1024L * 1024L * 1024L;
        if (current.availableBytes() != null && needed > current.availableBytes()) {
            throw new InvalidStorageConfigurationException("Requested volume exceeds pool free space.");
        }
        log.info("Creating libvirt volume {} in pool {} sizeGb={} format={}", volume, pool, request.sizeGb(), format);
        CommandResult result = libvirtCli.volCreateAs(pool, volume, request.sizeGb(), format);
        if (!result.succeeded()) {
            throw new StorageOperationException("vol-create-as failed: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        return describe(pool, true).volumes().stream()
                .filter(item -> item.name().equals(volume))
                .findFirst()
                .orElseThrow(() -> new StorageOperationException("Volume was created but libvirt did not list it"));
    }

    @Override
    public void deleteVolume(String poolName, String volumeName) {
        requireConnected();
        String pool = LibvirtNames.requireSafe(poolName, "pool");
        String volume = LibvirtNames.requireSafe(volumeName, "volume");
        VirtualStoragePool current = describe(pool, true);
        StorageVolume match = current.volumes().stream()
                .filter(item -> item.name().equals(volume))
                .findFirst()
                .orElseThrow(() -> new StorageNotFoundException("volume", volume));
        if (!match.usedBy().isEmpty()) {
            throw new StorageInUseException("Volume '" + volume + "' is attached to a VM. Detach it first.");
        }
        log.info("Deleting libvirt volume {} from pool {}", volume, pool);
        CommandResult result = libvirtCli.volDelete(pool, volume);
        if (!result.succeeded()) {
            throw new StorageOperationException("vol-delete failed: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    private VirtualStoragePool describe(String name, boolean includeVolumes) {
        CommandResult info = libvirtCli.poolInfo(name);
        if (!info.succeeded()) {
            throw new StorageNotFoundException("pool", name);
        }
        StoragePoolInfo parsed = LibvirtOutputParser.parsePoolInfo(name, info.stdout());
        boolean active = parsed.state() != null && (
                parsed.state().equalsIgnoreCase("running") || parsed.state().equalsIgnoreCase("active"));
        boolean persistent = yes(LibvirtOutputParser.parseKeyedField(info.stdout(), "Persistent"));
        boolean autostart = yes(LibvirtOutputParser.parseKeyedField(info.stdout(), "Autostart"));
        String uuid = LibvirtOutputParser.parseKeyedField(info.stdout(), "UUID");
        String type = "dir";
        String path = null;
        CommandResult xml = libvirtCli.poolDumpXml(name);
        if (xml.succeeded()) {
            PoolXmlParser.Addressing addressing = PoolXmlParser.parse(xml.stdout());
            type = addressing.type() != null ? addressing.type() : type;
            path = addressing.path();
            if (uuid == null) {
                uuid = addressing.uuid();
            }
        }
        List<StorageVolume> volumes = includeVolumes && active ? listVolumes(name) : List.of();
        return new VirtualStoragePool(
                name, uuid, parsed.state(), active, persistent, autostart, type, path,
                parsed.capacityBytes(), parsed.allocationBytes(), parsed.availableBytes(), volumes
        );
    }

    private List<StorageVolume> listVolumes(String pool) {
        CommandResult result = libvirtCli.volList(pool);
        if (!result.succeeded()) {
            return List.of();
        }
        List<StorageVolume> volumes = new ArrayList<>();
        for (LibvirtOutputParser.VolumeSummary summary : LibvirtOutputParser.parseVolList(result.stdout())) {
            String format = summary.name() != null && summary.name().endsWith(".qcow2") ? "qcow2" : "raw";
            volumes.add(new StorageVolume(
                    summary.name(), pool, summary.path(), summary.type(), format,
                    summary.capacityBytes(), summary.allocationBytes(), List.of()
            ));
        }
        return volumes;
    }

    private boolean exists(String name) {
        return libvirtCli.poolInfo(name).succeeded();
    }

    private void requireExisting(String name) {
        if (!exists(name)) {
            throw new StorageNotFoundException("pool", name);
        }
    }

    private boolean connected() {
        return hostMetricsProvider.collect().libvirt().connectionSucceeded();
    }

    private void requireConnected() {
        if (!connected()) {
            throw new InfrastructureConnectionException("libvirt is not connected to " + libvirtCli.uri());
        }
    }

    private static boolean yes(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith("yes");
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown error";
    }
}
