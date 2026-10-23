package com.minicloud.infrastructure.hypervisor;

import com.minicloud.common.exception.InfrastructureConnectionException;
import com.minicloud.common.exception.InterfaceNotFoundException;
import com.minicloud.common.exception.VmAlreadyExistsException;
import com.minicloud.common.exception.VmNotFoundException;
import com.minicloud.common.exception.VmOperationException;
import com.minicloud.config.MiniCloudProperties;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import com.minicloud.infrastructure.process.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Talks to libvirt for inventory and Phase 3 lifecycle.
 */
@Component
public class LibvirtHypervisorProvider implements HypervisorProvider {

    private static final Logger log = LoggerFactory.getLogger(LibvirtHypervisorProvider.class);

    private final MiniCloudProperties properties;
    private final HostMetricsProvider hostMetricsProvider;
    private final LibvirtCli libvirtCli;
    private final IsoCatalog isoCatalog;

    public LibvirtHypervisorProvider(
            MiniCloudProperties properties,
            HostMetricsProvider hostMetricsProvider,
            LibvirtCli libvirtCli,
            IsoCatalog isoCatalog
    ) {
        this.properties = properties;
        this.hostMetricsProvider = hostMetricsProvider;
        this.libvirtCli = libvirtCli;
        this.isoCatalog = isoCatalog;
    }

    @Override
    public HypervisorCapabilities capabilities() {
        HostSnapshot snapshot = hostMetricsProvider.collect();
        boolean kvm = "AVAILABLE".equals(snapshot.kvm().status());
        boolean connected = snapshot.libvirt().connectionSucceeded();
        String libvirtVersion = null;
        String hypervisorVersion = null;
        LibvirtNodeInfo node = null;
        if (connected) {
            CommandResult version = libvirtCli.version();
            if (version.succeeded()) {
                LibvirtVersionInfo parsed = LibvirtOutputParser.parseVersion(version.stdout());
                libvirtVersion = parsed.libvirtVersion();
                hypervisorVersion = parsed.hypervisorVersion();
            }
            CommandResult nodeinfo = libvirtCli.nodeinfo();
            if (nodeinfo.succeeded()) {
                node = LibvirtOutputParser.parseNodeinfo(nodeinfo.stdout());
            }
        }
        return new HypervisorCapabilities(
                properties.getHypervisor().getType(),
                properties.getHypervisor().getUri(),
                kvm,
                connected,
                snapshot.libvirt().status(),
                snapshot.libvirt().detail(),
                true,
                libvirtVersion,
                hypervisorVersion,
                node
        );
    }

    @Override
    public List<VirtualMachineInfo> listVms() {
        if (!connected()) {
            log.info("Skipping VM listing: libvirt is not connected");
            return List.of();
        }
        log.info("Listing libvirt domains via {}", libvirtCli.uri());
        CommandResult result = libvirtCli.listAll();
        if (!result.succeeded()) {
            log.warn("virsh list failed: {}", firstNonBlank(result.stderr(), result.stdout()));
            return List.of();
        }
        List<VirtualMachineInfo> listed = LibvirtOutputParser.parseList(result.stdout());
        List<VirtualMachineInfo> detailed = new ArrayList<>();
        for (VirtualMachineInfo summary : listed) {
            detailed.add(describe(summary.name(), summary));
        }
        log.info("libvirt reported {} domain(s)", detailed.size());
        return detailed;
    }

    @Override
    public VirtualMachineInfo getVm(String vmId) {
        LibvirtNames.requireSafe(vmId);
        requireConnected();
        CommandResult info = libvirtCli.dominfo(vmId);
        if (!info.succeeded()) {
            if (LibvirtOutputParser.looksLikeMissingDomain(new LibvirtOutputParser.CommandHint(info.stdout(), info.stderr()))) {
                throw new VmNotFoundException(vmId);
            }
            log.warn("virsh dominfo {} failed: {}", vmId, firstNonBlank(info.stderr(), info.stdout()));
            throw new VmNotFoundException(vmId);
        }
        VirtualMachineInfo base = VirtualMachineInfo.summary(vmId, null, "unknown", "UNKNOWN");
        return describe(vmId, LibvirtOutputParser.mergeDominfo(base, info.stdout()));
    }

    @Override
    public VirtualMachineInfo createVm(CreateVmRequest request) {
        requireConnected();
        String name = LibvirtNames.requireSafe(request.name());
        String network = LibvirtNames.requireSafe(request.network());
        String pool = LibvirtNames.requireSafe(properties.getHypervisor().getStoragePool());
        String volume = LibvirtNames.requireSafe(name + ".qcow2");

        if (exists(name)) {
            throw new VmAlreadyExistsException(name);
        }
        ensureStoragePool();
        requireNetwork(network);

        String isoPath = isoCatalog.resolve(request.operatingSystem()).orElse(null);
        if (isoPath != null && !Files.isRegularFile(Path.of(isoPath))) {
            isoPath = null;
        }

        log.info("Creating libvirt domain {} cpu={} memoryMb={} diskGb={} network={} iso={}",
                name, request.cpuCores(), request.memoryMb(), request.diskGb(), network, isoPath != null);

        CommandResult volumeResult = libvirtCli.volCreateAs(pool, volume, request.diskGb());
        if (!volumeResult.succeeded()) {
            throw new VmOperationException("Disk creation failed: " + firstNonBlank(volumeResult.stderr(), volumeResult.stdout()));
        }

        CommandResult pathResult = libvirtCli.volPath(pool, volume);
        if (!pathResult.succeeded() || pathResult.stdout().isBlank()) {
            libvirtCli.volDelete(pool, volume);
            throw new VmOperationException("Could not resolve the new volume path");
        }
        String diskPath = pathResult.stdout();

        String xml = DomainXmlFactory.kvmDomain(
                name,
                request.cpuCores(),
                request.memoryMb(),
                diskPath,
                isoPath,
                network,
                properties.getHypervisor().getEmulator()
        );
        CommandResult define = libvirtCli.define(xml);
        if (!define.succeeded()) {
            libvirtCli.volDelete(pool, volume);
            throw new VmOperationException("libvirt define failed: " + firstNonBlank(define.stderr(), define.stdout()));
        }
        log.info("VM created successfully in libvirt: {}", name);

        if (request.startAfterCreate()) {
            CommandResult start = libvirtCli.start(name);
            if (!start.succeeded()) {
                log.warn("Domain {} was defined but start failed: {}", name, firstNonBlank(start.stderr(), start.stdout()));
            } else {
                log.info("VM started successfully: {}", name);
            }
        }
        return getVm(name);
    }

    @Override
    public void startVm(String vmId) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId);
        requireExisting(name);
        log.info("Starting libvirt domain {}", name);
        CommandResult result = libvirtCli.start(name);
        if (!result.succeeded() && !containsIgnoreCase(result.stderr(), "already active")) {
            throw new VmOperationException("Failed to start VM: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("VM started successfully: {}", name);
    }

    @Override
    public void stopVm(String vmId) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId);
        requireExisting(name);
        log.info("Force-stopping libvirt domain {}", name);
        CommandResult result = libvirtCli.destroy(name);
        if (!result.succeeded() && !containsIgnoreCase(result.stderr(), "not running")
                && !containsIgnoreCase(result.stderr(), "domain is not running")) {
            throw new VmOperationException("Failed to stop VM: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("VM stopped successfully: {}", name);
    }

    @Override
    public void restartVm(String vmId) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId);
        VirtualMachineInfo current = getVm(name);
        log.info("Restarting libvirt domain {}", name);
        if ("RUNNING".equals(current.normalizedState())) {
            CommandResult reboot = libvirtCli.reboot(name);
            if (!reboot.succeeded()) {
                log.warn("ACPI reboot failed for {}, falling back to destroy+start", name);
                stopVm(name);
                startVm(name);
            }
        } else {
            startVm(name);
        }
        log.info("VM restart requested: {}", name);
    }

    @Override
    public void shutdownVm(String vmId) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId);
        requireExisting(name);
        log.info("ACPI shutdown requested for {}", name);
        CommandResult result = libvirtCli.shutdown(name);
        if (!result.succeeded()) {
            throw new VmOperationException("Failed to shutdown VM: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("VM shutdown requested: {}", name);
    }

    @Override
    public void deleteVm(String vmId) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId);
        requireExisting(name);
        log.info("Deleting libvirt domain {}", name);
        CommandResult info = libvirtCli.dominfo(name);
        if (info.succeeded()) {
            VirtualMachineInfo current = LibvirtOutputParser.mergeDominfo(
                    VirtualMachineInfo.summary(name, null, "unknown", "UNKNOWN"), info.stdout());
            if ("RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState())) {
                libvirtCli.destroy(name);
            }
        }
        CommandResult undefine = libvirtCli.undefine(name, true);
        if (!undefine.succeeded()) {
            CommandResult fallback = libvirtCli.undefine(name, false);
            String volume = name + ".qcow2";
            libvirtCli.volDelete(properties.getHypervisor().getStoragePool(), volume);
            if (!fallback.succeeded()) {
                throw new VmOperationException("Failed to delete VM: " + firstNonBlank(undefine.stderr(), fallback.stderr()));
            }
        }
        log.info("VM deleted successfully: {}", name);
    }

    @Override
    public void attachInterface(String vmId, String network, String model) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId, "domain");
        String networkName = LibvirtNames.requireSafe(network, "network");
        String nicModel = LibvirtNames.requireNicModel(model);
        requireExisting(name);
        requireNetwork(networkName);
        VirtualMachineInfo current = getVm(name);
        boolean live = "RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState());
        log.info("Attaching network {} to domain {} model={} live={}", networkName, name, nicModel, live);
        CommandResult result = libvirtCli.attachInterface(name, networkName, nicModel, live);
        if (!result.succeeded() && live) {
            log.warn("Live attach failed for {}, retrying persistent-only: {}", name, firstNonBlank(result.stderr(), result.stdout()));
            result = libvirtCli.attachInterface(name, networkName, nicModel, false);
        }
        if (!result.succeeded()) {
            throw new VmOperationException("Failed to attach interface: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("Attached network {} to {}", networkName, name);
    }

    @Override
    public void attachOvsInterface(String vmId, String bridge, String model) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId, "domain");
        String bridgeName = com.minicloud.infrastructure.ovs.OvsNames.requireSafe(bridge);
        String nicModel = LibvirtNames.requireNicModel(model);
        requireExisting(name);
        VirtualMachineInfo current = getVm(name);
        boolean live = "RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState());
        log.info("Attaching OVS bridge {} to domain {} model={} live={}", bridgeName, name, nicModel, live);
        CommandResult result = libvirtCli.attachOvsInterface(name, bridgeName, nicModel, live);
        if (!result.succeeded() && live) {
            log.warn("Live OVS attach failed for {}, retrying persistent-only: {}", name, firstNonBlank(result.stderr(), result.stdout()));
            result = libvirtCli.attachOvsInterface(name, bridgeName, nicModel, false);
        }
        if (!result.succeeded()) {
            throw new VmOperationException("Failed to attach OVS interface: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("Attached OVS bridge {} to {}", bridgeName, name);
    }

    @Override
    public void detachInterface(String vmId, String mac) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId, "domain");
        String nicMac = LibvirtNames.requireMac(mac);
        requireExisting(name);
        VirtualMachineInfo current = getVm(name);
        VmNicInfo match = current.nics().stream()
                .filter(nic -> nicMac.equalsIgnoreCase(nic.mac()))
                .findFirst()
                .orElse(null);
        if (match == null) {
            throw new InterfaceNotFoundException(name, nicMac);
        }
        String detachType = "bridge".equalsIgnoreCase(match.type()) ? "bridge" : "network";
        boolean live = "RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState());
        log.info("Detaching MAC {} from domain {} type={} live={}", nicMac, name, detachType, live);
        CommandResult config = libvirtCli.detachInterface(name, nicMac, detachType, false);
        boolean configGone = config.succeeded() || containsIgnoreCase(config.stderr(), "no interface");
        if (!configGone) {
            throw new VmOperationException("Failed to detach persistent interface: " + firstNonBlank(config.stderr(), config.stdout()));
        }
        if (live) {
            CommandResult liveResult = libvirtCli.detachInterface(name, nicMac, detachType, true);
            if (!liveResult.succeeded() && !containsIgnoreCase(liveResult.stderr(), "no interface")) {
                throw new VmOperationException("Failed to detach live interface: " + firstNonBlank(liveResult.stderr(), liveResult.stdout()));
            }
            if (stillHasMac(name, nicMac)) {
                throw new VmOperationException(
                        "libvirt removed the persistent NIC, but the running domain still has a TAP for MAC "
                                + nicMac + ". Restart the VM (or wait for the guest virtio driver) to finish unplug.");
            }
        }
        log.info("Detached MAC {} from {}", nicMac, name);
    }

    @Override
    public void attachDisk(String vmId, String path, String target, String format) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId, "domain");
        String diskPath = com.minicloud.infrastructure.storage.StoragePaths.requireSafeVolumePath(path);
        String diskTarget = LibvirtNames.requireDiskTarget(target);
        requireExisting(name);
        VirtualMachineInfo current = getVm(name);
        boolean used = current.disks().stream().anyMatch(disk -> diskTarget.equals(disk.target()) || diskPath.equals(disk.source()));
        if (used) {
            throw new VmOperationException("Domain already has target " + diskTarget + " or this volume is already attached");
        }
        boolean live = "RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState());
        log.info("Attaching disk {} to {} as {} live={}", diskPath, name, diskTarget, live);
        if (live) {
            CommandResult liveResult = libvirtCli.attachDisk(name, diskPath, diskTarget, format, true);
            if (!liveResult.succeeded()) {
                throw new VmOperationException("Failed to attach live disk: " + firstNonBlank(liveResult.stderr(), liveResult.stdout()));
            }
            CommandResult config = libvirtCli.attachDisk(name, diskPath, diskTarget, format, false);
            if (!config.succeeded()) {
                log.warn("Live disk attached but persistent XML update failed: {}", firstNonBlank(config.stderr(), config.stdout()));
            }
        } else {
            CommandResult result = libvirtCli.attachDisk(name, diskPath, diskTarget, format, false);
            if (!result.succeeded()) {
                throw new VmOperationException("Failed to attach disk: " + firstNonBlank(result.stderr(), result.stdout()));
            }
        }
        VirtualMachineInfo after = getVm(name);
        boolean visible = after.disks().stream().anyMatch(disk -> diskTarget.equals(disk.target()));
        if (!visible) {
            throw new VmOperationException(
                    "libvirt attach-disk returned success, but " + diskTarget + " is not in domblklist.");
        }
    }

    @Override
    public void attachRbdDisk(String vmId, String pool, String image, String target) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId, "domain");
        String safePool = com.minicloud.infrastructure.ceph.CephNames.requireSafe(pool, "pool");
        String safeImage = com.minicloud.infrastructure.ceph.CephNames.requireImage(image);
        String diskTarget = LibvirtNames.requireDiskTarget(target);
        requireExisting(name);
        ensureCephSecret();
        String needle = safePool + "/" + safeImage;
        VirtualMachineInfo current = getVm(name);
        boolean used = current.disks().stream().anyMatch(disk ->
                diskTarget.equals(disk.target())
                        || (disk.source() != null && disk.source().contains(needle)));
        if (used) {
            throw new VmOperationException("Domain already has target " + diskTarget + " or this RBD image is already attached");
        }
        String xml = com.minicloud.infrastructure.ceph.CephClientConfig.rbdDiskXml(
                safePool, safeImage, diskTarget,
                com.minicloud.infrastructure.ceph.CephClientConfig.monitorHosts(),
                com.minicloud.infrastructure.ceph.CephClientConfig.LIBVIRT_SECRET_UUID);
        boolean live = "RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState());
        log.info("Attaching RBD {} to {} as {} live={}", needle, name, diskTarget, live);
        if (live) {
            CommandResult liveResult = libvirtCli.attachDevice(name, xml, true, false);
            if (!liveResult.succeeded()) {
                throw new VmOperationException("Failed to attach live RBD disk: "
                        + firstNonBlank(liveResult.stderr(), liveResult.stdout()));
            }
            CommandResult config = libvirtCli.attachDevice(name, xml, false, true);
            if (!config.succeeded()) {
                log.warn("Live RBD attached but persistent XML update failed: {}",
                        firstNonBlank(config.stderr(), config.stdout()));
            }
        } else {
            CommandResult result = libvirtCli.attachDevice(name, xml, false, true);
            if (!result.succeeded()) {
                throw new VmOperationException("Failed to attach RBD disk: "
                        + firstNonBlank(result.stderr(), result.stdout()));
            }
        }
        VirtualMachineInfo after = getVm(name);
        boolean visible = after.disks().stream().anyMatch(disk -> diskTarget.equals(disk.target()));
        if (!visible) {
            throw new VmOperationException(
                    "libvirt attach-device returned success, but " + diskTarget + " is not in domblklist.");
        }
    }

    private void ensureCephSecret() {
        String uuid = com.minicloud.infrastructure.ceph.CephClientConfig.LIBVIRT_SECRET_UUID;
        CommandResult list = libvirtCli.secretList();
        boolean present = list.succeeded() && list.stdout() != null && list.stdout().contains(uuid);
        if (!present) {
            log.info("Defining libvirt Ceph secret {}", uuid);
            CommandResult define = libvirtCli.secretDefine(com.minicloud.infrastructure.ceph.CephClientConfig.secretXml());
            if (!define.succeeded() && !containsIgnoreCase(define.stderr(), "already")) {
                throw new VmOperationException("virsh secret-define failed: "
                        + firstNonBlank(define.stderr(), define.stdout()));
            }
        }
        String key = com.minicloud.infrastructure.ceph.CephClientConfig.adminKey();
        CommandResult set = libvirtCli.secretSetValue(uuid, key);
        if (!set.succeeded()) {
            throw new VmOperationException("virsh secret-set-value failed: "
                    + firstNonBlank(set.stderr(), set.stdout()));
        }
    }

    @Override
    public void detachDisk(String vmId, String target) {
        requireConnected();
        String name = LibvirtNames.requireSafe(vmId, "domain");
        String diskTarget = LibvirtNames.requireDiskTarget(target);
        if ("vda".equals(diskTarget)) {
            throw new VmOperationException("Refusing to detach the boot disk (vda).");
        }
        requireExisting(name);
        VirtualMachineInfo current = getVm(name);
        boolean present = current.disks().stream().anyMatch(disk -> diskTarget.equals(disk.target()));
        if (!present) {
            throw new VmOperationException("Domain '" + name + "' has no disk target " + diskTarget);
        }
        boolean live = "RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState());
        log.info("Detaching disk {} from {} live={}", diskTarget, name, live);
        CommandResult config = libvirtCli.detachDisk(name, diskTarget, false);
        boolean configGone = config.succeeded() || containsIgnoreCase(config.stderr(), "no disk")
                || containsIgnoreCase(config.stderr(), "not found");
        if (!configGone) {
            throw new VmOperationException("Failed to detach persistent disk: " + firstNonBlank(config.stderr(), config.stdout()));
        }
        if (live) {
            CommandResult liveResult = libvirtCli.detachDisk(name, diskTarget, true);
            if (!liveResult.succeeded()
                    && !containsIgnoreCase(liveResult.stderr(), "no disk")
                    && !containsIgnoreCase(liveResult.stderr(), "not found")) {
                throw new VmOperationException("Failed to detach live disk: " + firstNonBlank(liveResult.stderr(), liveResult.stdout()));
            }
            VirtualMachineInfo after = getVm(name);
            boolean still = after.disks().stream().anyMatch(disk -> diskTarget.equals(disk.target()));
            if (still) {
                throw new VmOperationException(
                        "libvirt removed the persistent disk, but the running domain still has "
                                + diskTarget + ". Restart the VM to finish unplug.");
            }
        }
    }

    private boolean stillHasMac(String name, String mac) {
        CommandResult ifaces = libvirtCli.ifList(name);
        if (!ifaces.succeeded()) {
            return true;
        }
        return LibvirtOutputParser.parseIfList(ifaces.stdout()).stream()
                .anyMatch(nic -> mac.equalsIgnoreCase(nic.mac()));
    }

    @Override
    public List<NetworkSummary> listNetworks() {
        if (!connected()) {
            return List.of();
        }
        CommandResult result = libvirtCli.netList();
        if (!result.succeeded()) {
            return List.of();
        }
        return LibvirtOutputParser.parseNetList(result.stdout());
    }

    @Override
    public StoragePoolInfo storagePool() {
        if (!connected()) {
            return null;
        }
        ensureStoragePool();
        String pool = properties.getHypervisor().getStoragePool();
        CommandResult result = libvirtCli.poolInfo(pool);
        if (!result.succeeded()) {
            return null;
        }
        return LibvirtOutputParser.parsePoolInfo(pool, result.stdout());
    }

    private void ensureStoragePool() {
        String pool = properties.getHypervisor().getStoragePool();
        CommandResult info = libvirtCli.poolInfo(pool);
        if (info.succeeded()) {
            return;
        }
        String target = properties.getHypervisor().getStoragePoolPath();
        if (target == null || !target.startsWith("/") || target.contains("..") || target.contains(" ")) {
            throw new VmOperationException("Refusing to create a storage pool at an unsafe path");
        }
        log.info("Defining libvirt storage pool {} at {}", pool, target);
        CommandResult define = libvirtCli.poolDefineAs(pool, target);
        if (!define.succeeded() && !containsIgnoreCase(define.stderr(), "already exists")) {
            throw new VmOperationException("Could not define storage pool: " + firstNonBlank(define.stderr(), define.stdout()));
        }
        libvirtCli.poolBuild(pool);
        CommandResult start = libvirtCli.poolStart(pool);
        if (!start.succeeded() && !containsIgnoreCase(start.stderr(), "already active")) {
            throw new VmOperationException("Could not start storage pool: " + firstNonBlank(start.stderr(), start.stdout()));
        }
        libvirtCli.poolAutostart(pool);
    }

    private void requireNetwork(String network) {
        boolean present = listNetworks().stream().anyMatch(item -> item.name().equals(network));
        if (!present) {
            throw new VmOperationException("libvirt has no network named '" + network + "'");
        }
    }

    private boolean exists(String name) {
        CommandResult info = libvirtCli.dominfo(name);
        return info.succeeded();
    }

    private void requireExisting(String name) {
        if (!exists(name)) {
            throw new VmNotFoundException(name);
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

    private VirtualMachineInfo describe(String name, VirtualMachineInfo seed) {
        VirtualMachineInfo current = seed;
        CommandResult info = libvirtCli.dominfo(name);
        if (info.succeeded()) {
            current = LibvirtOutputParser.mergeDominfo(current, info.stdout());
        }
        List<VmDiskInfo> disks = List.of();
        CommandResult blk = libvirtCli.blkList(name);
        if (blk.succeeded()) {
            disks = LibvirtOutputParser.parseBlkList(blk.stdout());
        }
        List<VmNicInfo> nics = List.of();
        CommandResult ifaces = libvirtCli.ifList(name);
        if (ifaces.succeeded()) {
            nics = LibvirtOutputParser.parseIfList(ifaces.stdout());
            CommandResult addrs = libvirtCli.ifAddr(name);
            if (addrs.succeeded()) {
                nics = LibvirtOutputParser.applyAddresses(nics, addrs.stdout());
            }
            nics = enrichNics(nics);
        }
        return new VirtualMachineInfo(
                current.name(),
                current.uuid(),
                current.hypervisorId(),
                current.state(),
                current.normalizedState(),
                current.cpuCores(),
                current.memoryBytes(),
                current.memoryUsedBytes(),
                current.osType(),
                current.persistent(),
                current.autostart(),
                disks,
                nics
        );
    }

    private List<VmNicInfo> enrichNics(List<VmNicInfo> nics) {
        Map<String, String> bridges = new LinkedHashMap<>();
        Map<String, String> leasesByMac = new LinkedHashMap<>();
        Set<String> leaseNetworks = new HashSet<>();
        for (VmNicInfo nic : nics) {
            if ("bridge".equalsIgnoreCase(nic.type())) {
                continue;
            }
            if (nic.network() == null || !LibvirtNames.isSafe(nic.network())) {
                continue;
            }
            bridges.computeIfAbsent(nic.network(), this::readBridge);
            if (leaseNetworks.add(nic.network())) {
                CommandResult leases = libvirtCli.netDhcpLeases(nic.network());
                if (leases.succeeded()) {
                    leasesByMac.putAll(LibvirtOutputParser.parseDhcpLeases(leases.stdout()));
                }
            }
        }
        List<VmNicInfo> enriched = new ArrayList<>();
        for (VmNicInfo nic : nics) {
            VmNicInfo next = nic;
            if ("bridge".equalsIgnoreCase(nic.type())) {
                next = nic.withBridge(nic.network());
            } else if (nic.network() != null) {
                next = nic.withBridge(bridges.get(nic.network()));
            }
            if (next.ip() == null && next.mac() != null) {
                String leaseIp = leasesByMac.get(next.mac().toLowerCase(Locale.ROOT));
                if (leaseIp != null) {
                    next = next.withIp(leaseIp);
                }
            }
            enriched.add(next);
        }
        return enriched;
    }

    private String readBridge(String network) {
        CommandResult info = libvirtCli.netInfo(network);
        if (!info.succeeded()) {
            return null;
        }
        return LibvirtOutputParser.parseKeyedField(info.stdout(), "Bridge");
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown error";
    }
}
