package com.minicloud.host.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hosts")
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 128)
    private String hostname;

    @Column(name = "os_name", length = 128)
    private String osName;

    @Column(name = "os_version", length = 256)
    private String osVersion;

    @Column(length = 128)
    private String kernel;

    @Column(length = 64)
    private String architecture;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "cpu_model", length = 256)
    private String cpuModel;

    @Column(name = "memory_bytes")
    private Long memoryBytes;

    @Column(name = "disk_bytes")
    private Long diskBytes;

    @Column(name = "is_local", nullable = false)
    private boolean local;

    @Column(name = "kvm_status", length = 32)
    private String kvmStatus;

    @Column(name = "libvirt_status", length = 32)
    private String libvirtStatus;

    @Column(name = "libvirt_uri", length = 128)
    private String libvirtUri;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getKernel() {
        return kernel;
    }

    public void setKernel(String kernel) {
        this.kernel = kernel;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public String getCpuModel() {
        return cpuModel;
    }

    public void setCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
    }

    public Long getMemoryBytes() {
        return memoryBytes;
    }

    public void setMemoryBytes(Long memoryBytes) {
        this.memoryBytes = memoryBytes;
    }

    public Long getDiskBytes() {
        return diskBytes;
    }

    public void setDiskBytes(Long diskBytes) {
        this.diskBytes = diskBytes;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String getKvmStatus() {
        return kvmStatus;
    }

    public void setKvmStatus(String kvmStatus) {
        this.kvmStatus = kvmStatus;
    }

    public String getLibvirtStatus() {
        return libvirtStatus;
    }

    public void setLibvirtStatus(String libvirtStatus) {
        this.libvirtStatus = libvirtStatus;
    }

    public String getLibvirtUri() {
        return libvirtUri;
    }

    public void setLibvirtUri(String libvirtUri) {
        this.libvirtUri = libvirtUri;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
