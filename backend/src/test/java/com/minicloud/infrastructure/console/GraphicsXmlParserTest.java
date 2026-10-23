package com.minicloud.infrastructure.console;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraphicsXmlParserTest {

    @Test
    void readsLoopbackVncPort() {
        GraphicsXmlParser.GraphicsDevice graphics = GraphicsXmlParser.parse("""
                <domain>
                  <devices>
                    <graphics type='vnc' port='5900' autoport='yes' listen='127.0.0.1'>
                      <listen type='address' address='127.0.0.1'/>
                    </graphics>
                  </devices>
                </domain>
                """);
        assertThat(graphics).isNotNull();
        assertThat(graphics.type()).isEqualTo("vnc");
        assertThat(graphics.listen()).isEqualTo("127.0.0.1");
        assertThat(graphics.port()).isEqualTo(5900);
        assertThat(graphics.autoport()).isTrue();
    }

    @Test
    void treatsMinusOneAsUnassigned() {
        GraphicsXmlParser.GraphicsDevice graphics = GraphicsXmlParser.parse("""
                <domain>
                  <devices>
                    <graphics type='vnc' port='-1' autoport='yes' listen='127.0.0.1'/>
                  </devices>
                </domain>
                """);
        assertThat(graphics).isNotNull();
        assertThat(graphics.port()).isNull();
    }
}
