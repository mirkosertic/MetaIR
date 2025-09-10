package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class MonitorExitTest {

    @Test
    public void testUsage() {
        final StringConstant a = new StringConstant("LOCK");
        final MonitorExit monitor = new MonitorExit(a);

        assertThat(monitor.arg0).isSameAs(a);

        assertThat(monitor.debugDescription()).isEqualTo("MonitorExit");

        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(monitor);
        assertThat(monitor.uses.size()).isEqualTo(1);
        assertThat(monitor.uses.getFirst().node()).isSameAs(a);
        assertThat(monitor.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(monitor.isConstant()).isFalse();
    }

    @Test
    public void fail_wrong_type() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new MonitorExit(new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Expecting non primitive type for monitorexit on stack, got int");
    }
}