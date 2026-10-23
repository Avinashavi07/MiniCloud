package com.minicloud.console;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.ConsoleUnavailableException;
import com.minicloud.console.dto.ConsoleListResponse;
import com.minicloud.console.dto.ConsoleSessionResponse;
import com.minicloud.console.service.ConsoleService;
import com.minicloud.console.service.ConsoleTicketRegistry;
import com.minicloud.infrastructure.console.ConsoleConnectionInfo;
import com.minicloud.infrastructure.console.ConsoleProvider;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsoleServiceTest {

    @Mock
    private ConsoleProvider consoleProvider;
    @Mock
    private HypervisorProvider hypervisorProvider;
    @Mock
    private AuditService auditService;

    private ConsoleService consoleService;

    @BeforeEach
    void setUp() {
        consoleService = new ConsoleService(consoleProvider, hypervisorProvider, new ConsoleTicketRegistry(), auditService);
    }

    @Test
    void listDoesNotInventEndpointsWhenDisconnected() {
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, false, "NOT_INSTALLED", "missing", false
        ));

        ConsoleListResponse list = consoleService.list();
        assertThat(list.listingAvailable()).isFalse();
        assertThat(list.consoles()).isEmpty();
    }

    @Test
    void openRefusesStoppedDomain() {
        when(hypervisorProvider.capabilities()).thenReturn(connected());
        when(consoleProvider.getConsole("ubuntu-test-02")).thenReturn(new ConsoleConnectionInfo(
                "ubuntu-test-02", "vnc", "127.0.0.1", null, null, false, "STOPPED",
                "Domain is not running. QEMU is not listening for VNC until the VM is started."
        ));

        assertThatThrownBy(() -> consoleService.open("ubuntu-test-02", "admin"))
                .isInstanceOf(ConsoleUnavailableException.class)
                .hasMessageContaining("not running");
    }

    @Test
    void openIssuesTicketForLoopbackVnc() {
        when(hypervisorProvider.capabilities()).thenReturn(connected());
        when(hypervisorProvider.listVms()).thenReturn(List.of(VirtualMachineInfo.summary(
                "ubuntu-test-02", "1", "running", "RUNNING")));
        when(consoleProvider.getConsole("ubuntu-test-02")).thenReturn(new ConsoleConnectionInfo(
                "ubuntu-test-02", "vnc", "127.0.0.1", 5900, 0, true, "AVAILABLE",
                "QEMU VNC is listening on 127.0.0.1:5900 (display :0)."));

        ConsoleSessionResponse session = consoleService.open("ubuntu-test-02", "admin");
        assertThat(session.ticket()).hasSize(32);
        assertThat(session.websocketPath()).startsWith("/ws/console?ticket=");
        assertThat(session.console().port()).isEqualTo(5900);
        assertThat(consoleService.list().consoles()).hasSize(1);
    }

    @Test
    void openRefusesNonLoopbackHost() {
        when(hypervisorProvider.capabilities()).thenReturn(connected());
        when(consoleProvider.getConsole("web-01")).thenReturn(new ConsoleConnectionInfo(
                "web-01", "vnc", "0.0.0.0", 5900, 0, true, "AVAILABLE", "open"));

        assertThatThrownBy(() -> consoleService.open("web-01", "admin"))
                .isInstanceOf(ConsoleUnavailableException.class)
                .hasMessageContaining("127.0.0.1");
    }

    private static HypervisorCapabilities connected() {
        return HypervisorCapabilities.of("libvirt", "qemu:///system", true, true, "CONNECTED", "ok", true);
    }
}
