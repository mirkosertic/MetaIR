package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MonitorEnterTest {

    @Test
    public void testUsage() {
        final StringConstant a = new StringConstant("LOCK");
        final MonitorEnter monitor = new MonitorEnter(a);

        assertThat(monitor.debugDescription()).isEqualTo("MonitorEnter");

        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(monitor);
        assertThat(monitor.uses.size()).isEqualTo(1);
        assertThat(monitor.uses.getFirst().node()).isSameAs(a);
        assertThat(monitor.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(monitor.isConstant()).isFalse();
    }
}