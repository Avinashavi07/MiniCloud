package com.minicloud.vm.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.InfrastructureConnectionException;
import com.minicloud.common.exception.InsufficientResourcesException;
import com.minicloud.common.exception.NetworkOperationException;
import com.minicloud.common.exception.StorageNotFoundException;
import com.minicloud.common.exception.StorageOperationException;
import com.minicloud.host.repository.HostRepository;
import com.minicloud.hypervisor.dto.VmListResponse;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import com.minicloud.infrastructure.hypervisor.CreateVmRequest;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.IsoCatalog;
import com.minicloud.infrastructure.hypervisor.StoragePoolInfo;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import com.minicloud.infrastructure.hypervisor.VmDiskInfo;
import com.minicloud.infrastructure.network.NetworkProvider;
import com.minicloud.infrastructure.network.VirtualNetworkInfo;
import com.minicloud.infrastructure.ceph.CephProvider;
import com.minicloud.infrastructure.ceph.RbdImageInfo;
import com.minicloud.infrastructure.ovs.OvsBridgeInfo;
import com.minicloud.infrastructure.ovs.OvsProvider;
import com.minicloud.infrastructure.storage.StorageProvider;
import com.minicloud.infrastructure.storage.StorageVolume;
import com.minicloud.infrastructure.storage.VirtualStoragePool;
import com.minicloud.vm.dto.AttachDiskRequestDto;
import com.minicloud.vm.dto.AttachInterfaceRequestDto;
import com.minicloud.vm.dto.AttachOvsInterfaceRequestDto;
import com.minicloud.vm.dto.AttachRbdDiskRequestDto;
import com.minicloud.vm.dto.CreateVmRequestDto;
import com.minicloud.vm.dto.VmCatalogResponse;
import com.minicloud.vm.entity.VirtualMachine;
import com.minicloud.vm.repository.VirtualMachineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class VmService {

    private static final Logger log = LoggerFactory.getLogger(VmService.class);

    private final HypervisorProvider hypervisorProvider;
    private final HostMetricsProvider hostMetricsProvider;
    private final HostRepository hostRepository;
    private final VirtualMachineRepository virtualMachineRepository;
    private final AuditService auditService;
    private final IsoCatalog isoCatalog;
    private final NetworkProvider networkProvider;
    private final StorageProvider storageProvider;
    private final OvsProvider ovsProvider;
    private final CephProvider cephProvider;

    public VmService(
            HypervisorProvider hypervisorProvider,
            HostMetricsProvider hostMetricsProvider,
            HostRepository hostRepository,
            VirtualMachineRepository virtualMachineRepository,
            AuditService auditService,
            IsoCatalog isoCatalog,
            NetworkProvider networkProvider,
            StorageProvider storageProvider,
            OvsProvider ovsProvider,
            CephProvider cephProvider
    ) {
        this.hypervisorProvider = hypervisorProvider;
        this.hostMetricsProvider = hostMetricsProvider;
        this.hostRepository = hostRepository;
        this.virtualMachineRepository = virtualMachineRepository;
        this.auditService = auditService;
        this.isoCatalog = isoCatalog;
        this.networkProvider = networkProvider;
        this.storageProvider = storageProvider;
        this.ovsProvider = ovsProvider;
        this.cephProvider = cephProvider;
    }

    public VmListResponse list() {
        HypervisorCapabilities capabilities = hypervisorProvider.capabilities();
        var vms = hypervisorProvider.listVms();
        String note = capabilities.connected()
                ? "These domains come from libvirt. MiniCloud metadata is stored only after a successful create."
                : "libvirt is not connected. MiniCloud will not invent VM records.";
        log.info("VM inventory requested: connected={} count={}", capabilities.connected(), vms.size());
        return new VmListResponse(capabilities.connected(), note, capabilities, vms);
    }

    public VirtualMachineInfo get(String name) {
        log.info("Reading libvirt domain {}", name);
        return hypervisorProvider.getVm(name);
    }

    public VmCatalogResponse catalog() {
        HostSnapshot host = hostMetricsProvider.collect();
        HypervisorCapabilities capabilities = hypervisorProvider.capabilities();
        boolean ubuntuIso = isoCatalog.resolve("ubuntu").isPresent();
        return new VmCatalogResponse(
                capabilities.connected(),
                capabilities.status(),
                host.cpuCores(),
                host.memoryAvailableBytes(),
                hypervisorProvider.storagePool(),
                hypervisorProvider.listNetworks(),
                List.of(
                        new VmCatalogResponse.OperatingSystemOption("ubuntu-24.04", "Ubuntu 24.04 (attach installer ISO if found)", ubuntuIso),
                        new VmCatalogResponse.OperatingSystemOption("none", "Blank disk — no installer ISO", true)
                ),
                isoCatalog.list()
        );
    }

    @Transactional
    public VirtualMachineInfo create(CreateVmRequestDto request, String username) {
        log.info("VM creation requested name={} cpu={} memoryMb={} diskGb={} os={} network={}",
                request.getName(), request.getCpuCores(), request.getMemoryMb(), request.getDiskGb(),
                request.getOperatingSystem(), request.getNetwork());
        HypervisorCapabilities capabilities = hypervisorProvider.capabilities();
        if (!capabilities.connected()) {
            throw new InfrastructureConnectionException("libvirt is not connected. Cannot create a real VM.");
        }
        if (!capabilities.kvmAvailable()) {
            throw new InsufficientResourcesException("KVM is not available on this host.");
        }
        validateResources(request);
        try {
            VirtualMachineInfo created = hypervisorProvider.createVm(new CreateVmRequest(
                    request.getName(),
                    request.getCpuCores(),
                    request.getMemoryMb(),
                    request.getDiskGb(),
                    request.getOperatingSystem(),
                    request.getNetwork(),
                    request.isStartAfterCreate()
            ));
            persistMetadata(request, created, username);
            auditService.record(username, "VM_CREATED", "VM", created.name(), "SUCCESS",
                    "Created libvirt domain " + created.name());
            return created;
        } catch (RuntimeException ex) {
            auditService.record(username, "VM_CREATED", "VM", request.getName(), "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public VirtualMachineInfo start(String name, String username) {
        return runAction(name, username, "VM_STARTED", "start", () -> hypervisorProvider.startVm(name));
    }

    public VirtualMachineInfo stop(String name, String username) {
        return runAction(name, username, "VM_STOPPED", "stop", () -> hypervisorProvider.stopVm(name));
    }

    public VirtualMachineInfo restart(String name, String username) {
        return runAction(name, username, "VM_RESTARTED", "restart", () -> hypervisorProvider.restartVm(name));
    }

    public VirtualMachineInfo shutdown(String name, String username) {
        return runAction(name, username, "VM_SHUTDOWN", "shutdown", () -> hypervisorProvider.shutdownVm(name));
    }

    public VirtualMachineInfo attachInterface(String name, AttachInterfaceRequestDto request, String username) {
        log.info("Attach interface requested vm={} network={} model={}", name, request.getNetwork(), request.getModel());
        VirtualNetworkInfo network = networkProvider.getNetwork(request.getNetwork());
        VirtualMachineInfo current = hypervisorProvider.getVm(name);
        if (("RUNNING".equals(current.normalizedState()) || "PAUSED".equals(current.normalizedState())) && !network.active()) {
            throw new NetworkOperationException("Network '" + network.name() + "' is not active. Start it before attaching a live NIC.");
        }
        return runAction(name, username, "VM_INTERFACE_ATTACHED", "attach-interface " + request.getNetwork(),
                () -> hypervisorProvider.attachInterface(name, request.getNetwork(), request.getModel()));
    }

    public VirtualMachineInfo attachOvsInterface(String name, AttachOvsInterfaceRequestDto request, String username) {
        log.info("Attach OVS interface requested vm={} bridge={} model={}", name, request.getBridge(), request.getModel());
        OvsBridgeInfo bridge = ovsProvider.getBridge(request.getBridge());
        return runAction(name, username, "VM_OVS_INTERFACE_ATTACHED", "attach-ovs " + bridge.name(),
                () -> hypervisorProvider.attachOvsInterface(name, bridge.name(), request.getModel()));
    }

    public VirtualMachineInfo detachInterface(String name, String mac, String username) {
        log.info("Detach interface requested vm={} mac={}", name, mac);
        return runAction(name, username, "VM_INTERFACE_DETACHED", "detach-interface " + mac,
                () -> hypervisorProvider.detachInterface(name, mac));
    }

    public VirtualMachineInfo attachDisk(String name, AttachDiskRequestDto request, String username) {
        log.info("Attach disk requested vm={} pool={} volume={}", name, request.getPool(), request.getVolume());
        VirtualStoragePool pool = storageProvider.getPool(request.getPool());
        if (!pool.active()) {
            throw new StorageOperationException("Pool '" + pool.name() + "' is not active. Start it before attaching a disk.");
        }
        StorageVolume volume = pool.volumes().stream()
                .filter(item -> item.name().equals(request.getVolume()))
                .findFirst()
                .orElseThrow(() -> new StorageNotFoundException("volume", request.getVolume()));
        VirtualMachineInfo current = hypervisorProvider.getVm(name);
        String target = (request.getTarget() == null || request.getTarget().isBlank())
                ? nextVirtioTarget(current)
                : request.getTarget();
        return runAction(name, username, "VM_DISK_ATTACHED", "attach-disk " + volume.name(),
                () -> hypervisorProvider.attachDisk(name, volume.path(), target, volume.format()));
    }

    public VirtualMachineInfo attachRbdDisk(String name, AttachRbdDiskRequestDto request, String username) {
        log.info("Attach RBD requested vm={} pool={} image={}", name, request.getPool(), request.getImage());
        RbdImageInfo image = cephProvider.getImage(request.getPool(), request.getImage());
        VirtualMachineInfo current = hypervisorProvider.getVm(name);
        String target = (request.getTarget() == null || request.getTarget().isBlank())
                ? nextVirtioTarget(current)
                : request.getTarget();
        return runAction(name, username, "VM_RBD_ATTACHED", "attach-rbd " + image.pool() + "/" + image.name(),
                () -> hypervisorProvider.attachRbdDisk(name, image.pool(), image.name(), target));
    }

    public VirtualMachineInfo detachDisk(String name, String target, String username) {
        log.info("Detach disk requested vm={} target={}", name, target);
        return runAction(name, username, "VM_DISK_DETACHED", "detach-disk " + target,
                () -> hypervisorProvider.detachDisk(name, target));
    }

    private static String nextVirtioTarget(VirtualMachineInfo vm) {
        Set<String> used = new HashSet<>();
        for (VmDiskInfo disk : vm.disks()) {
            if (disk.target() != null) {
                used.add(disk.target());
            }
        }
        for (char letter = 'b'; letter <= 'z'; letter++) {
            String candidate = "vd" + letter;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        throw new StorageOperationException("No free virtio disk target remains (vdb-vdz).");
    }

    @Transactional
    public void delete(String name, String username) {
        log.info("VM deletion requested for {}", name);
        try {
            hypervisorProvider.deleteVm(name);
            virtualMachineRepository.deleteByName(name);
            auditService.record(username, "VM_DELETED", "VM", name, "SUCCESS", "Deleted libvirt domain " + name);
        } catch (RuntimeException ex) {
            auditService.record(username, "VM_DELETED", "VM", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private VirtualMachineInfo runAction(String name, String username, String action, String verb, Runnable operation) {
        log.info("VM {} requested for {}", verb, name);
        try {
            operation.run();
            auditService.record(username, action, "VM", name, "SUCCESS", "libvirt " + verb + " " + name);
            return hypervisorProvider.getVm(name);
        } catch (RuntimeException ex) {
            auditService.record(username, action, "VM", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private void validateResources(CreateVmRequestDto request) {
        HostSnapshot host = hostMetricsProvider.collect();
        if (host.cpuCores() != null && request.getCpuCores() > host.cpuCores()) {
            throw new InsufficientResourcesException(
                    "Requested " + request.getCpuCores() + " vCPUs but the host has " + host.cpuCores());
        }
        long requestedMemory = request.getMemoryMb() * 1024L * 1024L;
        if (host.memoryAvailableBytes() != null && requestedMemory > host.memoryAvailableBytes()) {
            throw new InsufficientResourcesException("Requested memory exceeds host MemAvailable.");
        }
        StoragePoolInfo pool = hypervisorProvider.storagePool();
        long requestedDisk = request.getDiskGb() * 1024L * 1024L * 1024L;
        if (pool != null && pool.availableBytes() != null && requestedDisk > pool.availableBytes()) {
            throw new InsufficientResourcesException("Requested disk exceeds the libvirt storage pool free space.");
        }
    }

    private void persistMetadata(CreateVmRequestDto request, VirtualMachineInfo created, String username) {
        VirtualMachine entity = virtualMachineRepository.findByName(created.name()).orElseGet(VirtualMachine::new);
        entity.setName(created.name());
        entity.setLibvirtUuid(created.uuid());
        entity.setCpuCores(request.getCpuCores());
        entity.setMemoryMb(request.getMemoryMb());
        entity.setDiskGb(request.getDiskGb());
        entity.setNetworkName(request.getNetwork());
        entity.setOperatingSystem(request.getOperatingSystem());
        entity.setCreatedBy(username);
        hostRepository.findFirstByLocalTrue().ifPresent(entity::setHost);
        virtualMachineRepository.save(entity);
    }
}
