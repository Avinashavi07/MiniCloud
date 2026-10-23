package com.minicloud.storage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateVolumeRequestDto {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$", message = "must be a libvirt-safe volume name")
    private String name;

    @Min(1)
    @Max(2048)
    private long sizeGb = 10;

    @NotBlank
    @Pattern(regexp = "^(?i)(qcow2|raw)$", message = "must be qcow2 or raw")
    private String format = "qcow2";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSizeGb() {
        return sizeGb;
    }

    public void setSizeGb(long sizeGb) {
        this.sizeGb = sizeGb;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
