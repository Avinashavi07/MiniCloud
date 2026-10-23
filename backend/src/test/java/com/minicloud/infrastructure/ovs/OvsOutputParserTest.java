package com.minicloud.infrastructure.ovs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OvsOutputParserTest {

    @Test
    void parseVersionAndDb() {
        String stdout = """
                ovs-vsctl (Open vSwitch) 3.3.0
                DB Schema 8.3.0
                """;
        assertThat(OvsOutputParser.parseVersion(stdout)).isEqualTo("3.3.0");
        assertThat(OvsOutputParser.parseDbVersion(stdout)).isEqualTo("8.3.0");
    }

    @Test
    void parseNameListSkipsUnsafe() {
        assertThat(OvsOutputParser.parseNameList("mc-br0\nbr;hack\n")).containsExactly("mc-br0");
    }

    @Test
    void detectsManagedExternalIds() {
        assertThat(OvsOutputParser.externalIdsMarkManaged("{minicloud=\"true\"}")).isTrue();
        assertThat(OvsOutputParser.externalIdsMarkManaged("{}")).isFalse();
    }
}
