package com.minicloud.host.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.HostNotFoundException;
import com.minicloud.host.dto.HostMetricsResponse;
import com.minicloud.host.dto.HostResponse;
import com.minicloud.host.entity.Host;
import com.minicloud.host.repository.HostRepository;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class HostService {

    private static final Logger log = LoggerFactory.getLogger(HostService.class);

    private final HostRepository hostRepository;
    private final HostMetricsProvider hostMetricsProvider;
    private final AuditService auditService;

    public HostService(
            HostRepository hostRepository,
            HostMetricsProvider hostMetricsProvider,
            AuditService auditService
    ) {
        this.hostRepository = hostRepository;
        this.hostMetricsProvider = hostMetricsProvider;
        this.auditService = auditService;
    }

    @Transactional
    public HostResponse discoverLocalHost() {
        HostSnapshot snapshot = hostMetricsProvider.collect();
        Host host = hostRepository.findByHostname(snapshot.hostname()).orElseGet(Host::new);
        boolean created = host.getId() == null;
        applySnapshot(host, snapshot);
        Host saved = hostRepository.save(host);
        if (created) {
            auditService.record("system", "HOST_DISCOVERED", "HOST", saved.getId().toString(), "SUCCESS",
                    "Discovered local host " + saved.getHostname());
        }
        log.info(
                "Host discovery: hostname={} cores={} memoryBytes={} kvm={} libvirt={}",
                saved.getHostname(),
                saved.getCpuCores(),
                saved.getMemoryBytes(),
                saved.getKvmStatus(),
                saved.getLibvirtStatus()
        );
        return HostResponse.from(saved, HostMetricsResponse.from(snapshot));
    }

    @Transactional
    public List<HostResponse> listHosts() {
        HostSnapshot snapshot = hostMetricsProvider.collect();
        refreshLocalFromSnapshot(snapshot);
        HostMetricsResponse metrics = HostMetricsResponse.from(snapshot);
        return hostRepository.findAll().stream()
                .map(host -> HostResponse.from(host, host.isLocal() ? metrics : null))
                .toList();
    }

    @Transactional
    public HostResponse getHost(UUID id) {
        Host host = hostRepository.findById(id).orElseThrow(() -> new HostNotFoundException(id));
        HostMetricsResponse metrics = null;
        if (host.isLocal()) {
            HostSnapshot snapshot = hostMetricsProvider.collect();
            applySnapshot(host, snapshot);
            hostRepository.save(host);
            metrics = HostMetricsResponse.from(snapshot);
        }
        return HostResponse.from(host, metrics);
    }

    @Transactional
    public HostMetricsResponse metrics(UUID id) {
        Host host = hostRepository.findById(id).orElseThrow(() -> new HostNotFoundException(id));
        if (!host.isLocal()) {
            throw new HostNotFoundException(id);
        }
        HostSnapshot snapshot = hostMetricsProvider.collect();
        applySnapshot(host, snapshot);
        hostRepository.save(host);
        return HostMetricsResponse.from(snapshot);
    }

    private void refreshLocalFromSnapshot(HostSnapshot snapshot) {
        hostRepository.findByHostname(snapshot.hostname()).ifPresent(host -> {
            applySnapshot(host, snapshot);
            hostRepository.save(host);
        });
    }

    private void applySnapshot(Host host, HostSnapshot snapshot) {
        host.setHostname(snapshot.hostname());
        host.setOsName(snapshot.osName());
        host.setOsVersion(snapshot.osVersion());
        host.setKernel(snapshot.kernel());
        host.setArchitecture(snapshot.architecture());
        host.setCpuCores(snapshot.cpuCores());
        host.setCpuModel(snapshot.cpuModel());
        host.setMemoryBytes(snapshot.memoryTotalBytes());
        host.setDiskBytes(snapshot.diskTotalBytes());
        host.setLocal(true);
        host.setKvmStatus(snapshot.kvm().status());
        host.setLibvirtStatus(snapshot.libvirt().status());
        host.setLibvirtUri(snapshot.libvirt().uri());
        host.setLastSeenAt(Instant.now());
        if (host.getCreatedAt() == null) {
            host.setCreatedAt(Instant.now());
        }
    }
}
