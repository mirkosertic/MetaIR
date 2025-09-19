package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtractThisRefProjectionTest {

    @Test
    public void testUsage() {
        final Method m = new Method();
        final ExtractThisRefProjection a = new ExtractThisRefProjection(IRType.CD_String, m);

        assertThat(a.sideeffectFree()).isTrue();

        assertThat(a.type).isEqualTo(IRType.CD_String);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(1);
        assertThat(a.uses.getFirst().node()).isSameAs(m);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(a.debugDescription()).isEqualTo("this : String");
        assertThat(a.name()).isEqualTo("this");

        assertThat(a.isConstant()).isTrue();
    }
}