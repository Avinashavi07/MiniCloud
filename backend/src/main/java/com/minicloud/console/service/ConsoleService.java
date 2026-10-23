package com.minicloud.console.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.ConsoleUnavailableException;
import com.minicloud.common.exception.InfrastructureConnectionException;
import com.minicloud.console.dto.ConsoleListResponse;
import com.minicloud.console.dto.ConsoleSessionResponse;
import com.minicloud.infrastructure.console.ConsoleConnectionInfo;
import com.minicloud.infrastructure.console.ConsoleProvider;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsoleService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleService.class);

    private final ConsoleProvider consoleProvider;
    private final HypervisorProvider hypervisorProvider;
    private final ConsoleTicketRegistry tickets;
    private final AuditService auditService;

    public ConsoleService(
            ConsoleProvider consoleProvider,
            HypervisorProvider hypervisorProvider,
            ConsoleTicketRegistry tickets,
            AuditService auditService
    ) {
        this.consoleProvider = consoleProvider;
        this.hypervisorProvider = hypervisorProvider;
        this.tickets = tickets;
        this.auditService = auditService;
    }

    public ConsoleListResponse list() {
        boolean connected = hypervisorProvider.capabilities().connected();
        if (!connected) {
            return new ConsoleListResponse(
                    false,
                    "libvirt is not connected. MiniCloud will not invent VNC endpoints.",
                    List.of());
        }
        List<ConsoleConnectionInfo> consoles = hypervisorProvider.listVms().stream()
                .map(vm -> consoleProvider.getConsole(vm.name()))
                .toList();
        log.info("Console inventory requested: connected={} count={}", connected, consoles.size());
        return new ConsoleListResponse(
                true,
                "Console endpoints come from libvirt (virsh vncdisplay / domain XML). QEMU VNC listens on 127.0.0.1; the browser reaches it through MiniCloud.",
                consoles);
    }

    public ConsoleConnectionInfo get(String name) {
        requireConnected();
        log.info("Reading libvirt console for {}", name);
        return consoleProvider.getConsole(name);
    }

    public ConsoleSessionResponse open(String name, String username) {
        requireConnected();
        ConsoleConnectionInfo console = consoleProvider.getConsole(name);
        if (!console.available() || console.port() == null) {
            auditService.record(username, "CONSOLE_OPENED", "CONSOLE", name, "FAILED", console.detail());
            throw new ConsoleUnavailableException(console.detail());
        }
        if (!isLoopback(console.host())) {
            throw new ConsoleUnavailableException(
                    "Refusing to proxy VNC at " + console.host() + ". MiniCloud only bridges 127.0.0.1.");
        }
        ConsoleTicketRegistry.Ticket ticket = tickets.issue(username, console);
        auditService.record(username, "CONSOLE_OPENED", "CONSOLE", name, "SUCCESS",
                "Issued VNC ticket for " + console.host() + ":" + console.port());
        return new ConsoleSessionResponse(
                console,
                ticket.id(),
                "/ws/console?ticket=" + ticket.id(),
                ticket.expiresAtEpochMs());
    }

    private void requireConnected() {
        if (!hypervisorProvider.capabilities().connected()) {
            throw new InfrastructureConnectionException("libvirt is not connected. Cannot read a real console.");
        }
    }

    public static boolean isLoopback(String host) {
        return host != null && (host.equals("127.0.0.1") || host.equals("localhost") || host.equals("::1"));
    }
}
