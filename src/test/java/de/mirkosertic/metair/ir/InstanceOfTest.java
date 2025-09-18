package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class InstanceOfTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final InstanceOf cc = new InstanceOf(a, b);

        assertThat(cc.sideeffectFree()).isTrue();

        assertThat(cc.debugDescription()).isEqualTo("InstanceOf");
        assertThat(cc.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(cc).isInstanceOf(Value.class);

        assertThat(a.usedBy).containsExactly(cc);
        assertThat(b.usedBy).containsExactly(cc);
        assertThat(cc.uses.size()).isEqualTo(2);
        assertThat(cc.uses.get(0).node()).isSameAs(a);
        assertThat(cc.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(cc.uses.get(1).node()).isSameAs(b);
        assertThat(cc.uses.get(1).use()).isEqualTo(new ArgumentUse(1));

        assertThat(MetaIRTestHelper.toDebugExpression(cc)).isEqualTo("(10 instanceof 20)");
    }
}