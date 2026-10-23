package com.minicloud.ovs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateOvsBridgeRequestDto {

    @NotBlank
    @Pattern(regexp = "^mc-[A-Za-z0-9][A-Za-z0-9_.-]{0,11}$", message = "must be mc-* and libvirt/OVS-safe (max 15 chars)")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
