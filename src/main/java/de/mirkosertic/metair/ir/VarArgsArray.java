package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.List;

public class VarArgsArray extends Value {

    VarArgsArray(final ClassDesc componentType, final List<Value> arguments) {
        super(componentType.arrayType());

        for (int i = 0; i < arguments.size(); i++) {
            use(arguments.get(i), new ArgumentUse(i));
        }
    }

    @Override
    public String debugDescription() {
        return "VarArgsArray : " + TypeUtils.toString(type);
    }
}
