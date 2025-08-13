package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MethodTypeTest {

    @Test
    public void testUsage() {
        final MethodType a = new MethodType(MethodTypeDesc.of(ConstantDescs.CD_String, List.of(ConstantDescs.CD_int)));

        assertThat(a.type).isEqualTo(MethodTypeDesc.of(ConstantDescs.CD_String, List.of(ConstantDescs.CD_int)));
        assertThat(a).isInstanceOf(ConstantValue.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("MethodType : (int)String");
    }

}