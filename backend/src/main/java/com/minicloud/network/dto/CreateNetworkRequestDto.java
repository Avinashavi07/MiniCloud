package com.minicloud.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateNetworkRequestDto {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$", message = "must be a libvirt-safe name")
    private String name;

    @NotBlank
    @Pattern(regexp = "^\\d{1,3}(?:\\.\\d{1,3}){3}/\\d{1,2}$", message = "must look like 192.168.100.0/24")
    private String cidr;

    @NotBlank
    @Pattern(regexp = "^\\d{1,3}(?:\\.\\d{1,3}){3}$", message = "must be an IPv4 address")
    private String gateway;

    @NotBlank
    @Pattern(regexp = "^\\d{1,3}(?:\\.\\d{1,3}){3}$", message = "must be an IPv4 address")
    private String dhcpStart;

    @NotBlank
    @Pattern(regexp = "^\\d{1,3}(?:\\.\\d{1,3}){3}$", message = "must be an IPv4 address")
    private String dhcpEnd;

    private boolean autostart = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getDhcpStart() {
        return dhcpStart;
    }

    public void setDhcpStart(String dhcpStart) {
        this.dhcpStart = dhcpStart;
    }

    public String getDhcpEnd() {
        return dhcpEnd;
    }

    public void setDhcpEnd(String dhcpEnd) {
        this.dhcpEnd = dhcpEnd;
    }

    public boolean isAutostart() {
        return autostart;
    }

    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }
}
