package com.minicloud.infrastructure.console;

/**
 * Display endpoint libvirt reports for a domain. Host is QEMU's listen
 * address (127.0.0.1), not a public VDI URL.
 */
public record ConsoleConnectionInfo(
        String vmName,
        String protocol,
        String host,
        Integer port,
        Integer display,
        boolean available,
        String status,
        String detail
) {
}
