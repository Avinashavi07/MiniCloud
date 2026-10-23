package com.minicloud.vm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateVmRequestDto {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$", message = "must be a libvirt-safe name")
    private String name;

    @Min(1)
    @Max(64)
    private int cpuCores;

    @Min(128)
    @Max(262144)
    private long memoryMb;

    @Min(1)
    @Max(2048)
    private long diskGb;

    @NotBlank
    @Pattern(regexp = "^(ubuntu|ubuntu-24.04|none)$", message = "must be ubuntu, ubuntu-24.04, or none")
    private String operatingSystem;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$")
    private String network;

    private boolean startAfterCreate = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public boolean isStartAfterCreate() {
        return startAfterCreate;
    }

    public void setStartAfterCreate(boolean startAfterCreate) {
        this.startAfterCreate = startAfterCreate;
    }
}
