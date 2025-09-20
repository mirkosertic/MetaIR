package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InvokeVirtualTest {

    @Test
    public void testUsage() {
        final ResolverContext resolverContext = new ResolverContext();
        final Value target = new StringConstant("hello");
        final InvokeVirtual a = new InvokeVirtual(IRType.CD_String, target, "bar", resolverContext.resolveMethodType(MethodTypeDesc.of(ConstantDescs.CD_void, List.of(ConstantDescs.CD_int))), List.of(new PrimitiveInt(10)));

        assertThat(a.type).isEqualTo(IRType.CD_void);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(2);
        assertThat(a.uses.getFirst().node()).isSameAs(target);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(a.isConstant()).isFalse();

        assertThat(a.debugDescription()).isEqualTo("Invoke virtual bar : (int)void");

        assertThat(MetaIRTestHelper.toDebugExpression(a)).isEqualTo("(\"hello\".bar(10))");
    }
}