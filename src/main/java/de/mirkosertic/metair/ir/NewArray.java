package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

public class NewArray extends Value {

    NewArray(final ClassDesc componentType, final Value length) {
        super(componentType.arrayType());

        if (!ConstantDescs.CD_int.equals(length.type)) {
            illegalArgument("Array length must be int, but was " + TypeUtils.toString(length.type));
        }

        use(length, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "NewArray : " + TypeUtils.toString(type);
    }
}
