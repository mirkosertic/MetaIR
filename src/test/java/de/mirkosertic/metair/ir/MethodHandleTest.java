package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import static org.assertj.core.api.Assertions.assertThat;

class MethodHandleTest {

    @Test
    public void testUsage() {
        final MethodHandle a = new MethodHandle(new IRType.MethodHandle(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, ClassDesc.of(MethodHandleTest.class.getName()), "testUsage", MethodTypeDesc.of(ConstantDescs.CD_void))));

        assertThat(a.sideeffectFree()).isTrue();

        assertThat(a.type).isEqualTo(new IRType.MethodHandle(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, ClassDesc.of(MethodHandleTest.class.getName()), "testUsage", MethodTypeDesc.of(ConstantDescs.CD_void))));
        assertThat(a).isInstanceOf(ConstantValue.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("MethodHandle : invokeInterface : MethodHandleTest.testUsage (MethodHandleTest)void");
    }

}