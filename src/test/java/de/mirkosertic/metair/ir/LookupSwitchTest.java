package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

class LookupSwitchTest {

    @Test
    public void  testUsage() {
        final Value value = new PrimitiveInt(10);
        final LookupSwitch a = new LookupSwitch(value, "defaultlabel", List.of(20));

        assertThat(a.arg0).isSameAs(value);

        assertThat(a.uses).hasSize(1);
        assertThat(a.uses.getFirst().node()).isSameAs(value);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(a.debugDescription()).isEqualTo("LookupSwitch");

        assertThat(a.usedBy).containsExactlyInAnyOrder(a.getNamedNode("default"), a.getNamedNode("case0"));

        assertThat(a.defaultProjection().name()).isEqualTo("default");
        assertThat(a.caseProjection(0).name()).isEqualTo("case0");
    }

    @Test
    public void fail_wrong_type() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            final Value value = new PrimitiveLong(10L);
            final LookupSwitch a = new LookupSwitch(value, "defaultlabel", List.of(20));
            fail("Exception expected");
        }).withMessage("Cannot use non int value of type long as switch value");
    }

}