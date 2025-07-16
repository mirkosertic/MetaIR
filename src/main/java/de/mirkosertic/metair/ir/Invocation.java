package de.mirkosertic.metair.ir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

public class Invocation extends Value {

    public final MethodInsnNode ins;

    Invocation(final MethodInsnNode ins, final List<Value> arguments) {
        super(Type.getReturnType(ins.desc));
        this.ins = ins;
        int index = 0;
        for (final Value v : arguments) {
            use(v, new ArgumentUse(index++));
        }
    }

    public boolean isInvokeStatic() {
        return ins.getOpcode() == Opcodes.INVOKESTATIC;
    }

    public boolean isInvokeSpecial() {
        return ins.getOpcode() == Opcodes.INVOKESPECIAL;
    }

    public boolean isInvokeVirtual() {
        return ins.getOpcode() == Opcodes.INVOKEVIRTUAL;
    }

    public boolean isInvokeInterface() {
        return ins.getOpcode() == Opcodes.INVOKEINTERFACE;
    }

    public boolean isInvokeDynamic() {
        return ins.getOpcode() == Opcodes.INVOKEDYNAMIC;
    }

    @Override
    public String debugDescription() {
        if (isInvokeStatic()) {
            return "Invoke static " + ins.name + " : " + ins.desc;
        } else if (isInvokeSpecial()) {
            return "Invoke special " + ins.name + " : " + ins.desc;
        } else if (isInvokeVirtual()) {
            return "Invoke virtual " + ins.name + " : " + ins.desc;
        } else if (isInvokeInterface()) {
            return "Invoke interface " + ins.name + " : " + ins.desc;
        } else if (isInvokeDynamic()) {
            return "Invoke dynamic " + ins.name + " : " + ins.desc;
        } else {
            throw new IllegalArgumentException("Unknown opcode " + ins.getOpcode());
        }
    }
}
