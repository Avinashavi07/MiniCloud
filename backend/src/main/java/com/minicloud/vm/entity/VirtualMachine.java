package com.minicloud.vm.entity;

import com.minicloud.host.entity.Host;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "virtual_machines")
public class VirtualMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "libvirt_uuid", length = 64)
    private String libvirtUuid;

    @ManyToOne
    @JoinColumn(name = "host_id")
    private Host host;

    @Column(name = "cpu_cores", nullable = false)
    private int cpuCores;

    @Column(name = "memory_mb", nullable = false)
    private long memoryMb;

    @Column(name = "disk_gb", nullable = false)
    private long diskGb;

    @Column(name = "network_name", length = 64)
    private String networkName;

    @Column(name = "operating_system", length = 64)
    private String operatingSystem;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLibvirtUuid() {
        return libvirtUuid;
    }

    public void setLibvirtUuid(String libvirtUuid) {
        this.libvirtUuid = libvirtUuid;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    public long getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(long memoryMb) {
        this.memoryMb = memoryMb;
    }

    public long getDiskGb() {
        return diskGb;
    }

    public void setDiskGb(long diskGb) {
        this.diskGb = diskGb;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
