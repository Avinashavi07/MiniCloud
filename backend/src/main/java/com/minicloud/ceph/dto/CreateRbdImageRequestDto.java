package com.minicloud.ceph.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateRbdImageRequestDto {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$")
    private String name;

    @Min(1)
    @Max(2048)
    private long sizeGb = 1;

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
}
