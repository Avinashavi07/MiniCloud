package com.minicloud.ceph.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.ceph.dto.CephListResponse;
import com.minicloud.ceph.dto.CreateCephPoolRequestDto;
import com.minicloud.ceph.dto.CreateRbdImageRequestDto;
import com.minicloud.common.exception.CephInUseException;
import com.minicloud.infrastructure.ceph.CephCapabilities;
import com.minicloud.infrastructure.ceph.CephClusterStatus;
import com.minicloud.infrastructure.ceph.CephPoolInfo;
import com.minicloud.infrastructure.ceph.CephProvider;
import com.minicloud.infrastructure.ceph.CreateCephPoolRequest;
import com.minicloud.infrastructure.ceph.CreateRbdImageRequest;
import com.minicloud.infrastructure.ceph.RbdImageInfo;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import com.minicloud.infrastructure.hypervisor.VmDiskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CephService {

    private static final Logger log = LoggerFactory.getLogger(CephService.class);

    private final CephProvider cephProvider;
    private final HypervisorProvider hypervisorProvider;
    private final AuditService auditService;

    public CephService(CephProvider cephProvider, HypervisorProvider hypervisorProvider, AuditService auditService) {
        this.cephProvider = cephProvider;
        this.hypervisorProvider = hypervisorProvider;
        this.auditService = auditService;
    }

    public CephListResponse list() {
        CephCapabilities capabilities = cephProvider.capabilities();
        String note = capabilities.connected()
                ? "Pools and RBD images come from Ceph (ceph/rbd). Directory pools stay on /storage."
                : capabilities.detail();
        CephClusterStatus status = null;
        List<CephPoolInfo> pools = List.of();
        if (capabilities.connected()) {
            status = cephProvider.status();
            pools = withUsage(cephProvider.listPools());
        }
        log.info("Ceph inventory requested: status={} pools={}", capabilities.status(), pools.size());
        return new CephListResponse(capabilities, status, note, pools);
    }

    public CephPoolInfo getPool(String name) {
        log.info("Reading Ceph pool {}", name);
        CephPoolInfo pool = cephProvider.getPool(name);
        List<RbdImageInfo> images = pool.images().stream().map(this::withUsage).toList();
        return pool.withImages(images);
    }

    public CephPoolInfo createPool(CreateCephPoolRequestDto request, String username) {
        log.info("Ceph pool creation requested name={}", request.getName());
        try {
            CephPoolInfo created = cephProvider.createPool(new CreateCephPoolRequest(request.getName(), request.getPgNum()));
            auditService.record(username, "CEPH_POOL_CREATED", "CEPH", created.name(), "SUCCESS",
                    "Created Ceph pool " + created.name());
            return created;
        } catch (RuntimeException ex) {
            auditService.record(username, "CEPH_POOL_CREATED", "CEPH", request.getName(), "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public void deletePool(String name, String username) {
        log.info("Ceph pool deletion requested for {}", name);
        try {
            cephProvider.deletePool(name);
            auditService.record(username, "CEPH_POOL_DELETED", "CEPH", name, "SUCCESS", "Deleted Ceph pool " + name);
        } catch (RuntimeException ex) {
            auditService.record(username, "CEPH_POOL_DELETED", "CEPH", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public RbdImageInfo createImage(String pool, CreateRbdImageRequestDto request, String username) {
        log.info("RBD image creation requested pool={} name={}", pool, request.getName());
        try {
            RbdImageInfo created = cephProvider.createImage(
                    new CreateRbdImageRequest(pool, request.getName(), request.getSizeGb()));
            auditService.record(username, "RBD_IMAGE_CREATED", "CEPH", pool + "/" + created.name(), "SUCCESS",
                    "Created RBD image " + pool + "/" + created.name());
            return withUsage(created);
        } catch (RuntimeException ex) {
            auditService.record(username, "RBD_IMAGE_CREATED", "CEPH", pool + "/" + request.getName(), "FAILED",
                    ex.getMessage());
            throw ex;
        }
    }

    public void deleteImage(String pool, String image, String username) {
        log.info("RBD image deletion requested {}/{}", pool, image);
        try {
            List<String> users = findUsers(pool, image);
            if (!users.isEmpty()) {
                throw new CephInUseException(
                        "RBD image '" + pool + "/" + image + "' is attached to VM(s) " + users + ". Detach first.");
            }
            cephProvider.deleteImage(pool, image);
            auditService.record(username, "RBD_IMAGE_DELETED", "CEPH", pool + "/" + image, "SUCCESS",
                    "Deleted RBD image " + pool + "/" + image);
        } catch (RuntimeException ex) {
            auditService.record(username, "RBD_IMAGE_DELETED", "CEPH", pool + "/" + image, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private List<CephPoolInfo> withUsage(List<CephPoolInfo> pools) {
        List<CephPoolInfo> enriched = new ArrayList<>();
        for (CephPoolInfo pool : pools) {
            try {
                List<RbdImageInfo> images = cephProvider.listImages(pool.name()).stream()
                        .map(this::withUsage)
                        .toList();
                enriched.add(pool.withImages(images));
            } catch (RuntimeException ex) {
                log.debug("Could not list images for {}: {}", pool.name(), ex.getMessage());
                enriched.add(pool);
            }
        }
        return enriched;
    }

    private RbdImageInfo withUsage(RbdImageInfo image) {
        return image.withUsedBy(findUsers(image.pool(), image.name()));
    }

    private List<String> findUsers(String pool, String image) {
        String needle = pool + "/" + image;
        List<String> users = new ArrayList<>();
        try {
            for (VirtualMachineInfo vm : hypervisorProvider.listVms()) {
                for (VmDiskInfo disk : vm.disks()) {
                    String source = disk.source();
                    if (source != null && (source.equals(needle) || source.contains(needle))) {
                        users.add(vm.name());
                        break;
                    }
                }
            }
        } catch (RuntimeException ex) {
            log.debug("Could not correlate RBD {} with VMs: {}", needle, ex.getMessage());
        }
        return users;
    }
}
