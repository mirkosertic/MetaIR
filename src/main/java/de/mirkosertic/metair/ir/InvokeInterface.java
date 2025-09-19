package de.mirkosertic.metair.ir;

import java.util.List;

public class InvokeInterface extends Invoke {

    public InvokeInterface(final IRType.MetaClass ownerType, final Value target, final String name, final IRType.MethodType methodTypeDesc, final List<Value> arguments) {
        super(ownerType, target, name, methodTypeDesc, arguments);
    }

    @Override
    public String debugDescription() {
        return "Invoke interface " + name + " : " + TypeUtils.toString(typeDesc);
    }

}
