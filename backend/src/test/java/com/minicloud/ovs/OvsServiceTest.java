package com.minicloud.ovs;

import com.minicloud.audit.service.AuditService;
import com.minicloud.infrastructure.ovs.OvsBridgeInfo;
import com.minicloud.infrastructure.ovs.OvsCapabilities;
import com.minicloud.infrastructure.ovs.OvsProvider;
import com.minicloud.ovs.dto.OvsListResponse;
import com.minicloud.ovs.service.OvsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvsServiceTest {

    @Mock
    private OvsProvider ovsProvider;
    @Mock
    private AuditService auditService;

    private OvsService ovsService;

    @BeforeEach
    void setUp() {
        ovsService = new OvsService(ovsProvider, auditService);
    }

    @Test
    void listDoesNotInventBridgesWhenDisconnected() {
        when(ovsProvider.capabilities()).thenReturn(new OvsCapabilities(
                false, false, "NOT_INSTALLED", "ovs-vsctl missing", null, null));

        OvsListResponse list = ovsService.list();
        assertThat(list.capabilities().connected()).isFalse();
        assertThat(list.bridges()).isEmpty();
    }

    @Test
    void listReturnsLiveBridgesWhenConnected() {
        when(ovsProvider.capabilities()).thenReturn(new OvsCapabilities(
                true, true, "CONNECTED", "ok", "3.3.0", "8.3.0"));
        when(ovsProvider.listBridges()).thenReturn(List.of(
                new OvsBridgeInfo("mc-br0", "uuid", true, List.of(), "0x1", "managed")));

        OvsListResponse list = ovsService.list();
        assertThat(list.bridges()).extracting(OvsBridgeInfo::name).containsExactly("mc-br0");
    }
}
