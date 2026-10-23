package com.minicloud.storage.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.InfrastructureConnectionException;
import com.minicloud.common.exception.StorageInUseException;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VmDiskInfo;
import com.minicloud.infrastructure.storage.ConnectedDiskInfo;
import com.minicloud.infrastructure.storage.CreatePoolRequest;
import com.minicloud.infrastructure.storage.CreateVolumeRequest;
import com.minicloud.infrastructure.storage.StorageProvider;
import com.minicloud.infrastructure.storage.StorageVolume;
import com.minicloud.infrastructure.storage.VirtualStoragePool;
import com.minicloud.storage.dto.CreatePoolRequestDto;
import com.minicloud.storage.dto.CreateVolumeRequestDto;
import com.minicloud.storage.dto.HostFilesystemInfo;
import com.minicloud.storage.dto.StorageListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final StorageProvider storageProvider;
    private final HypervisorProvider hypervisorProvider;
    private final HostMetricsProvider hostMetricsProvider;
    private final AuditService auditService;

    public StorageService(
            StorageProvider storageProvider,
            HypervisorProvider hypervisorProvider,
            HostMetricsProvider hostMetricsProvider,
            AuditService auditService
    ) {
        this.storageProvider = storageProvider;
        this.hypervisorProvider = hypervisorProvider;
        this.hostMetricsProvider = hostMetricsProvider;
        this.auditService = auditService;
    }

    public StorageListResponse list() {
        boolean connected = hypervisorProvider.capabilities().connected();
        List<VirtualStoragePool> pools = storageProvider.listPools().stream()
                .map(this::withUsage)
                .toList();
        String note = connected
                ? "Pools and volumes come from libvirt. The host filesystem below is Linux, not a virtual disk."
                : "libvirt is not connected. MiniCloud will not invent storage pools.";
        log.info("Storage inventory requested: connected={} pools={}", connected, pools.size());
        return new StorageListResponse(connected, note, List.of(hostFilesystem()), pools);
    }

    public VirtualStoragePool get(String name) {
        log.info("Reading libvirt storage pool {}", name);
        return withUsage(storageProvider.getPool(name));
    }

    public VirtualStoragePool createPool(CreatePoolRequestDto request, String username) {
        log.info("Storage pool creation requested name={} path={}", request.getName(), request.getPath());
        requireConnected();
        try {
            VirtualStoragePool created = storageProvider.createPool(new CreatePoolRequest(
                    request.getName(), request.getPath(), request.isAutostart()));
            auditService.record(username, "STORAGE_POOL_CREATED", "STORAGE", created.name(), "SUCCESS",
                    "Created libvirt pool " + created.name());
            return withUsage(created);
        } catch (RuntimeException ex) {
            auditService.record(username, "STORAGE_POOL_CREATED", "STORAGE", request.getName(), "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public VirtualStoragePool start(String name, String username) {
        return runPoolAction(name, username, "STORAGE_POOL_STARTED", "start", () -> storageProvider.startPool(name));
    }

    public VirtualStoragePool stop(String name, String username) {
        VirtualStoragePool current = withUsage(storageProvider.getPool(name));
        if (current.volumes().stream().anyMatch(volume -> !volume.usedBy().isEmpty())) {
            throw new StorageInUseException("Pool '" + name + "' has volumes attached to VMs. Detach them first.");
        }
        return runPoolAction(name, username, "STORAGE_POOL_STOPPED", "stop", () -> storageProvider.stopPool(name));
    }

    public void deletePool(String name, String username) {
        log.info("Storage pool deletion requested for {}", name);
        VirtualStoragePool current = withUsage(storageProvider.getPool(name));
        if (!current.volumes().isEmpty()) {
            throw new StorageInUseException("Pool '" + name + "' still has volumes. Delete them first.");
        }
        try {
            storageProvider.deletePool(name);
            auditService.record(username, "STORAGE_POOL_DELETED", "STORAGE", name, "SUCCESS", "Deleted libvirt pool " + name);
        } catch (RuntimeException ex) {
            auditService.record(username, "STORAGE_POOL_DELETED", "STORAGE", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public VirtualStoragePool setAutostart(String name, boolean enabled, String username) {
        return runPoolAction(name, username, "STORAGE_POOL_AUTOSTART", "autostart=" + enabled,
                () -> storageProvider.setAutostart(name, enabled));
    }

    public StorageVolume createVolume(String pool, CreateVolumeRequestDto request, String username) {
        log.info("Volume creation requested pool={} name={} sizeGb={}", pool, request.getName(), request.getSizeGb());
        requireConnected();
        try {
            StorageVolume created = storageProvider.createVolume(new CreateVolumeRequest(
                    pool, request.getName(), request.getSizeGb(), request.getFormat()));
            auditService.record(username, "STORAGE_VOLUME_CREATED", "VOLUME", created.name(), "SUCCESS",
                    "Created volume " + created.name() + " in " + pool);
            return withUsage(created);
        } catch (RuntimeException ex) {
            auditService.record(username, "STORAGE_VOLUME_CREATED", "VOLUME", request.getName(), "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public void deleteVolume(String pool, String volume, String username) {
        log.info("Volume deletion requested pool={} volume={}", pool, volume);
        VirtualStoragePool current = withUsage(storageProvider.getPool(pool));
        StorageVolume match = current.volumes().stream()
                .filter(item -> item.name().equals(volume))
                .findFirst()
                .orElse(null);
        if (match != null && !match.usedBy().isEmpty()) {
            throw new StorageInUseException("Volume '" + volume + "' is attached to a VM. Detach it first.");
        }
        try {
            storageProvider.deleteVolume(pool, volume);
            auditService.record(username, "STORAGE_VOLUME_DELETED", "VOLUME", volume, "SUCCESS",
                    "Deleted volume " + volume + " from " + pool);
        } catch (RuntimeException ex) {
            auditService.record(username, "STORAGE_VOLUME_DELETED", "VOLUME", volume, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private VirtualStoragePool runPoolAction(String name, String username, String action, String verb, Runnable operation) {
        try {
            operation.run();
            auditService.record(username, action, "STORAGE", name, "SUCCESS", "libvirt " + verb + " " + name);
            return withUsage(storageProvider.getPool(name));
        } catch (RuntimeException ex) {
            auditService.record(username, action, "STORAGE", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private VirtualStoragePool withUsage(VirtualStoragePool pool) {
        List<ConnectedDiskInfo> attachments = attachments();
        List<StorageVolume> volumes = pool.volumes().stream()
                .map(volume -> volume.withUsedBy(attachments.stream()
                        .filter(item -> volume.path() != null && volume.path().equals(item.source()))
                        .toList()))
                .toList();
        return pool.withVolumes(volumes);
    }

    private StorageVolume withUsage(StorageVolume volume) {
        return volume.withUsedBy(attachments().stream()
                .filter(item -> volume.path() != null && volume.path().equals(item.source()))
                .toList());
    }

    private List<ConnectedDiskInfo> attachments() {
        List<ConnectedDiskInfo> disks = new ArrayList<>();
        hypervisorProvider.listVms().forEach(vm -> {
            for (VmDiskInfo disk : vm.disks()) {
                if (disk.source() != null) {
                    disks.add(new ConnectedDiskInfo(vm.name(), disk.target(), disk.source()));
                }
            }
        });
        return disks;
    }

    private HostFilesystemInfo hostFilesystem() {
        HostSnapshot snapshot = hostMetricsProvider.collect();
        return new HostFilesystemInfo(
                "host-root-filesystem",
                snapshot.diskMount(),
                "host-disk",
                snapshot.diskTotalBytes(),
                snapshot.diskAvailableBytes(),
                "This is the Ubuntu host filesystem, not a libvirt storage pool."
        );
    }

    private void requireConnected() {
        if (!hypervisorProvider.capabilities().connected()) {
            throw new InfrastructureConnectionException("libvirt is not connected. Cannot change real storage.");
        }
    }
}
