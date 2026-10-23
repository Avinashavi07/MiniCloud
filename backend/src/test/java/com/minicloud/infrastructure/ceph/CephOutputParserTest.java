package com.minicloud.infrastructure.ceph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CephOutputParserTest {

    @Test
    void parsesVersion() {
        assertThat(CephOutputParser.parseVersion("ceph version 19.2.3 (abc) squid"))
                .isEqualTo("19.2.3");
    }

    @Test
    void parsesHealthAndCountsFromStatus() {
        String stdout = """
                cluster:
                  id:     8af478c7-c989-4294-8b3d-db29215f5d37
                  health: HEALTH_OK

                services:
                  mon: 1 daemons, quorum Avinash
                  mgr: Avinash(active)
                  osd: 3 osds: 3 up, 3 in

                data:
                  pools:   1 pools, 8 pgs
                  usage:   82 MiB used, 12 GiB / 12 GiB avail
                """;
        CephClusterStatus status = CephOutputParser.parseStatusText(stdout);
        assertThat(status.health()).isEqualTo("HEALTH_OK");
        assertThat(status.monCount()).isEqualTo(1);
        assertThat(status.osdCount()).isEqualTo(3);
        assertThat(status.osdUp()).isEqualTo(3);
        assertThat(status.usedBytes()).isGreaterThan(0);
    }

    @Test
    void parsesPoolDetailsAndDf() {
        String details = """
                [
                  {"pool":1,"pool_name":"mc-rbd","pg_num":8,"application_metadata":{"rbd":{}}}
                ]
                """;
        List<CephPoolInfo> pools = new ArrayList<>(CephOutputParser.parsePoolDetailsJson(details));
        assertThat(pools).hasSize(1);
        assertThat(pools.getFirst().managedByMiniCloud()).isTrue();

        String df = """
                {"pools":[{"name":"mc-rbd","stats":{"stored":1024,"max_avail":4096,"objects":2}}]}
                """;
        CephOutputParser.applyDfStats(pools, df);
        assertThat(pools.getFirst().storedBytes()).isEqualTo(1024);
        assertThat(pools.getFirst().objectCount()).isEqualTo(2);
    }

    @Test
    void parsesRbdInfo() {
        String info = """
                {"name":"data-01","size":1073741824,"format":2}
                """;
        RbdImageInfo image = CephOutputParser.parseRbdInfoJson("mc-rbd", info);
        assertThat(image).isNotNull();
        assertThat(image.name()).isEqualTo("data-01");
        assertThat(image.sizeBytes()).isEqualTo(1073741824L);
    }
}
