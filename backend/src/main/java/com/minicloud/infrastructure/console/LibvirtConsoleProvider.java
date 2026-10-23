package com.minicloud.infrastructure.console;

import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.LibvirtCli;
import com.minicloud.infrastructure.hypervisor.LibvirtNames;
import com.minicloud.infrastructure.hypervisor.LibvirtOutputParser;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import com.minicloud.infrastructure.process.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Source of truth is libvirt: domain XML graphics + {@code virsh vncdisplay}.
 */
@Component
public class LibvirtConsoleProvider implements ConsoleProvider {

    private static final Logger log = LoggerFactory.getLogger(LibvirtConsoleProvider.class);

    private final HypervisorProvider hypervisorProvider;
    private final LibvirtCli libvirtCli;

    public LibvirtConsoleProvider(HypervisorProvider hypervisorProvider, LibvirtCli libvirtCli) {
        this.hypervisorProvider = hypervisorProvider;
        this.libvirtCli = libvirtCli;
    }

    @Override
    public ConsoleConnectionInfo getConsole(String vmId) {
        String name = LibvirtNames.requireSafe(vmId, "domain");
        VirtualMachineInfo vm = hypervisorProvider.getVm(name);
        boolean running = "RUNNING".equals(vm.normalizedState()) || "PAUSED".equals(vm.normalizedState());

        CommandResult dump = libvirtCli.dumpXml(name);
        GraphicsXmlParser.GraphicsDevice graphics = dump.succeeded()
                ? GraphicsXmlParser.parse(dump.stdout())
                : null;

        if (graphics == null) {
            return new ConsoleConnectionInfo(
                    name, null, null, null, null, false, "MISSING",
                    "Domain XML has no <graphics> device. QEMU is not serving a console.");
        }

        String protocol = graphics.type() == null ? "unknown" : graphics.type().toLowerCase();
        if (!"vnc".equals(protocol)) {
            return new ConsoleConnectionInfo(
                    name, protocol, graphics.listen(), graphics.port(), displayOf(graphics.port()),
                    false, "UNSUPPORTED",
                    "Domain graphics are '" + protocol + "'. Phase 6 only proxies QEMU VNC.");
        }

        if (!running) {
            return new ConsoleConnectionInfo(
                    name, protocol, firstNonBlank(graphics.listen(), "127.0.0.1"),
                    null, null, false, "STOPPED",
                    "Domain is not running. QEMU is not listening for VNC until the VM is started.");
        }

        CommandResult vnc = libvirtCli.vncDisplay(name);
        LibvirtOutputParser.VncDisplay display = vnc.succeeded()
                ? LibvirtOutputParser.parseVncDisplay(vnc.stdout())
                : null;
        String host = display != null ? display.host() : firstNonBlank(graphics.listen(), "127.0.0.1");
        Integer port = display != null ? display.port() : graphics.port();
        Integer number = display != null ? display.display() : displayOf(port);

        if (port == null) {
            log.warn("Running domain {} has VNC graphics but no assigned port", name);
            return new ConsoleConnectionInfo(
                    name, protocol, host, null, number, false, "NO_PORT",
                    "libvirt did not assign a VNC port. Check virsh vncdisplay " + name + ".");
        }

        return new ConsoleConnectionInfo(
                name, protocol, host, port, number, true, "AVAILABLE",
                "QEMU VNC is listening on " + host + ":" + port + " (display :" + number + ").");
    }

    private static Integer displayOf(Integer port) {
        if (port == null || port < 5900) {
            return null;
        }
        return port - 5900;
    }

    private static String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
