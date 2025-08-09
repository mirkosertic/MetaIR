package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

public class InvocationVirtual extends Invocation {

    public InvocationVirtual(final ClassDesc ownerType, final Value target, final String name, final MethodTypeDesc methodTypeDesc, final List<Value> arguments) {
        super(ownerType, target, name, methodTypeDesc, arguments);
    }

    @Override
    public String debugDescription() {
        return "Invoke virtual " + name + " : " + TypeUtils.toString(typeDesc);
    }

}
