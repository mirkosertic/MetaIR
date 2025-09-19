package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GetStaticTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference v = new RuntimeclassReference(IRType.CD_String);
        final GetStatic get = new GetStatic(v, "fieldname", IRType.CD_int);

        assertThat(v.usedBy).containsExactly(get);

        assertThat(get.uses).hasSize(1);
        assertThat(get.uses.getFirst().node()).isSameAs(v);
        assertThat(get.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(get.type).isEqualTo(IRType.CD_int);
        assertThat(get).isInstanceOf(Value.class);
        assertThat(get.debugDescription()).isEqualTo("GetStaticField : fieldname : int");
        assertThat(get.fieldName).isEqualTo("fieldname");
        assertThat(get.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(get)).isEqualTo("(String.class.fieldname)");
    }
}