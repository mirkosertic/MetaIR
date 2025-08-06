package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class ArrayLoad extends Value {

    public final ClassDesc arrayType;

    ArrayLoad(final ClassDesc elementType, final ClassDesc arrayType, final Value array, final Value index) {
        super(elementType);
        this.arrayType = arrayType;

        use(array, new ArgumentUse(0));
        use(index, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "ArrayLoad : " + TypeUtils.toString(type);
    }
}
