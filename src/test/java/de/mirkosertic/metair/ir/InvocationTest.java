package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class InvocationTest {

    @Test
    public void testUsage() {
        final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "foo", "bar", "()V", false);
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.type).isEqualTo(Type.VOID_TYPE);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isFalse();

        assertThat(a.peepholeOptimization()).isEmpty();
    }

    @Test
    void isInvokeStatic() {
        final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "foo", "bar", "()V", false);
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isTrue();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke static bar : ()V");
    }

    @Test
    void isInvokeSpecial() {
        final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKESPECIAL, "foo", "bar", "()V", false);
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isTrue();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke special bar : ()V");
    }

    @Test
    void isInvokeVirtual() {
        final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "foo", "bar", "()V", false);
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isTrue();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke virtual bar : ()V");
    }

    @Test
    void isInvokeInterface() {
        final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKEINTERFACE, "foo", "bar", "()V", false);
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isTrue();
        assertThat(a.isInvokeDynamic()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Invoke interface bar : ()V");
    }

    @Test
    void isInvokeDynamic() {
        final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKEDYNAMIC, "foo", "bar", "()V", false);
        final Invocation a = new Invocation(node, new ArrayList<>());

        assertThat(a.isInvokeStatic()).isFalse();
        assertThat(a.isInvokeSpecial()).isFalse();
        assertThat(a.isInvokeVirtual()).isFalse();
        assertThat(a.isInvokeInterface()).isFalse();
        assertThat(a.isInvokeDynamic()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("Invoke dynamic bar : ()V");
    }
}