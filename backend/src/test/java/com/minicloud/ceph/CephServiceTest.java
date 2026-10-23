package com.minicloud.ceph;

import com.minicloud.audit.service.AuditService;
import com.minicloud.ceph.dto.CephListResponse;
import com.minicloud.ceph.service.CephService;
import com.minicloud.infrastructure.ceph.CephCapabilities;
import com.minicloud.infrastructure.ceph.CephPoolInfo;
import com.minicloud.infrastructure.ceph.CephProvider;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CephServiceTest {

    @Mock
    private CephProvider cephProvider;
    @Mock
    private HypervisorProvider hypervisorProvider;
    @Mock
    private AuditService auditService;

    private CephService cephService;

    @BeforeEach
    void setUp() {
        cephService = new CephService(cephProvider, hypervisorProvider, auditService);
    }

    @Test
    void listDoesNotInventPoolsWhenDisconnected() {
        when(cephProvider.capabilities()).thenReturn(new CephCapabilities(
                false, false, "NOT_INSTALLED", "ceph missing", null, null));

        CephListResponse list = cephService.list();
        assertThat(list.capabilities().connected()).isFalse();
        assertThat(list.pools()).isEmpty();
        assertThat(list.status()).isNull();
    }

    @Test
    void listReturnsLivePoolsWhenConnected() {
        when(cephProvider.capabilities()).thenReturn(new CephCapabilities(
                true, true, "CONNECTED", "ok", "19.2.3", "fsid"));
        when(cephProvider.status()).thenReturn(new com.minicloud.infrastructure.ceph.CephClusterStatus(
                "HEALTH_OK", "ok", 1, 3, 3, 3, 0, 0, 0, List.of("mon", "osd"), "raw"));
        when(cephProvider.listPools()).thenReturn(List.of(
                new CephPoolInfo("mc-rbd", 1, 8, "rbd", true, 0, 0, 0, List.of(), "managed")));
        when(cephProvider.listImages("mc-rbd")).thenReturn(List.of());

        CephListResponse list = cephService.list();
        assertThat(list.pools()).extracting(CephPoolInfo::name).containsExactly("mc-rbd");
        assertThat(list.status().health()).isEqualTo("HEALTH_OK");
    }
}
