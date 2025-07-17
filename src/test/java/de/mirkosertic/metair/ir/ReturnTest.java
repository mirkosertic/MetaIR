package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReturnTest {

    @Test
    public void testUsage() {
        final Return ret = new Return();

        assertThat(ret.uses.size()).isEqualTo(0);
        assertThat(ret.usedBy).isEmpty();
        assertThat(ret.isConstant()).isFalse();

        assertThat(ret.peepholeOptimization()).isEmpty();

        assertThat(ret.debugDescription()).isEqualTo("Return");
    }
}