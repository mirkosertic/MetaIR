package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class ArrayStore extends Node {

    public final ClassDesc arrayType;

    ArrayStore(final ClassDesc arrayType, final Value array, final Value index, final Value value) {
        this.arrayType = arrayType;
        use(array, new ArgumentUse(0));
        use(index, new ArgumentUse(1));
        use(value, new ArgumentUse(2));
    }

    @Override
    public String debugDescription() {
        return "ArrayStore : " + TypeUtils.toString(arrayType);
    }
}
