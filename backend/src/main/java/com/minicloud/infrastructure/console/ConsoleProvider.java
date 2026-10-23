package com.minicloud.infrastructure.console;

/**
 * VDI / console boundary. Phase 6 reads QEMU VNC from libvirt.
 * MiniCloud does not speak RFB or RDP. A later GuacamoleConsoleProvider
 * can replace this adapter without changing controllers.
 */
public interface ConsoleProvider {

    ConsoleConnectionInfo getConsole(String vmId);
}
