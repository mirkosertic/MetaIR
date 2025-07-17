package de.mirkosertic.metair.ir;

import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.List;

public class Invocation extends Value {

    public final InvokeInstruction ins;

    Invocation(final InvokeInstruction ins, final List<Value> arguments) {
        super(ins.typeSymbol().returnType());
        this.ins = ins;
        int index = 0;
        for (final Value v : arguments) {
            use(v, new ArgumentUse(index++));
        }
    }

    public boolean isInvokeStatic() {
        return ins.opcode() == Opcode.INVOKESTATIC;
    }

    public boolean isInvokeSpecial() {
        return ins.opcode() == Opcode.INVOKESPECIAL;
    }

    public boolean isInvokeVirtual() {
        return ins.opcode() == Opcode.INVOKEVIRTUAL;
    }

    public boolean isInvokeInterface() {
        return ins.opcode() == Opcode.INVOKEINTERFACE;
    }

    public boolean isInvokeDynamic() {
        return ins.opcode() == Opcode.INVOKEDYNAMIC;
    }

    @Override
    public String debugDescription() {
        if (isInvokeStatic()) {
            return "Invoke static " + ins.name() + " : " + DebugUtils.toString(ins.typeSymbol());
        } else if (isInvokeSpecial()) {
            return "Invoke special " + ins.name() + " : " + DebugUtils.toString(ins.typeSymbol());
        } else if (isInvokeVirtual()) {
            return "Invoke virtual " + ins.name() + " : " + DebugUtils.toString(ins.typeSymbol());
        } else if (isInvokeInterface()) {
            return "Invoke interface " + ins.name() + " : " + DebugUtils.toString(ins.typeSymbol());
        } else if (isInvokeDynamic()) {
            return "Invoke dynamic " + ins.name() + " : " + DebugUtils.toString(ins.typeSymbol());
        } else {
            throw new IllegalArgumentException("Unknown opcode " + ins.opcode());
        }
    }
}
