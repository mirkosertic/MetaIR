package de.mirkosertic.metair.ir;

import java.util.List;

public class InvokeSpecial extends Invoke {

    public InvokeSpecial(final IRType.MetaClass ownerType, final Value target, final String name, final IRType.MethodType methodTypeDesc, final List<Value> arguments) {
        super(ownerType, target, name, methodTypeDesc, arguments);
    }

    @Override
    public String debugDescription() {
        return "Invoke special " + name + " : " + TypeUtils.toString(typeDesc);
    }
}
