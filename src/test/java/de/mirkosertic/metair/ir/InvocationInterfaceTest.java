package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InvocationInterfaceTest {

    @Test
    public void testUsage() {
        final Value target = new StringConstant("hello");
        final InvocationInterface a = new InvocationInterface(ConstantDescs.CD_String, target, "bar", MethodTypeDesc.of(ConstantDescs.CD_void, List.of(ConstantDescs.CD_int)), List.of(new PrimitiveInt(10)));

        assertThat(a.type).isEqualTo(ConstantDescs.CD_void);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(2);
        assertThat(a.uses.getFirst().node()).isSameAs(target);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(a.isConstant()).isFalse();

        assertThat(a.debugDescription()).isEqualTo("Invoke interface bar : (int)void");
    }
}