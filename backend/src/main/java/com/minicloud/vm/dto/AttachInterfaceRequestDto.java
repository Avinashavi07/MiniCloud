package com.minicloud.vm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AttachInterfaceRequestDto {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$", message = "must be a libvirt-safe network name")
    private String network;

    @NotBlank
    @Pattern(regexp = "^(?i)(virtio|e1000|rtl8139)$", message = "must be virtio, e1000, or rtl8139")
    private String model = "virtio";

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
