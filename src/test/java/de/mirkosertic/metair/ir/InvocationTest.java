package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class InvocationTest {

    @Test
    public void testUsage() {
        final InvokeInstruction node = InvokeInstruction.of(Opcode.INVOKESPECIAL, ConstantPoolBuilder.of().methodRefEntry(ConstantDescs.CD_String, "bar", MethodTypeDesc.of(ConstantDescs.CD_void)));
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.type).isEqualTo(ConstantDescs.CD_void);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isFalse();

        assertThat(a.peepholeOptimization()).isEmpty();
    }

    @Test
    void isInvokeStatic() {
        final InvokeInstruction node = InvokeInstruction.of(Opcode.INVOKESTATIC, ConstantPoolBuilder.of().methodRefEntry(ConstantDescs.CD_String, "bar", MethodTypeDesc.of(ConstantDescs.CD_void)));
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isTrue();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke static bar : ()void");
    }

    @Test
    void isInvokeSpecial() {
        final InvokeInstruction node = InvokeInstruction.of(Opcode.INVOKESPECIAL, ConstantPoolBuilder.of().methodRefEntry(ConstantDescs.CD_String, "bar", MethodTypeDesc.of(ConstantDescs.CD_void)));
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isTrue();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke special bar : ()void");
    }

    @Test
    void isInvokeVirtual() {
        final InvokeInstruction node = InvokeInstruction.of(Opcode.INVOKEVIRTUAL, ConstantPoolBuilder.of().methodRefEntry(ConstantDescs.CD_String, "bar", MethodTypeDesc.of(ConstantDescs.CD_void)));
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isTrue();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke virtual bar : ()void");
    }

    @Test
    void isInvokeInterface() {
        final InvokeInstruction node = InvokeInstruction.of(Opcode.INVOKEINTERFACE, ConstantPoolBuilder.of().methodRefEntry(ConstantDescs.CD_String, "bar", MethodTypeDesc.of(ConstantDescs.CD_void)));
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isTrue();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke interface bar : ()void");
    }

/*    @Test
    void isInvokeDynamic() {
        final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKEDYNAMIC, "foo", "bar", "()V", false);
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("Invoke dynamic bar : ()V");
    }*/
}