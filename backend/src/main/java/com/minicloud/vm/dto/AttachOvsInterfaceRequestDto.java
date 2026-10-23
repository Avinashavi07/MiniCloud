package com.minicloud.vm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AttachOvsInterfaceRequestDto {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{0,14}$", message = "must be an OVS-safe bridge name")
    private String bridge;

    @NotBlank
    @Pattern(regexp = "^(?i)(virtio|e1000|rtl8139)$", message = "must be virtio, e1000, or rtl8139")
    private String model = "virtio";

    public String getBridge() {
        return bridge;
    }

    public void setBridge(String bridge) {
        this.bridge = bridge;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
