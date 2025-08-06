package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class NewArray extends Value {

    NewArray(final ClassDesc componentType, final Value length) {
        super(componentType.arrayType());
        use(length, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "NewArray : " + TypeUtils.toString(type);
    }
}
