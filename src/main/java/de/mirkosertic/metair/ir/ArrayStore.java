package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

public class ArrayStore extends Node {

    public final ClassDesc arrayType;

    ArrayStore(final Value array, final Value index, final Value value) {
        if (!array.type.isArray()) {
            illegalArgument("Cannot store to non array of type " + TypeUtils.toString(array.type));
        }

        if (!index.type.equals(ConstantDescs.CD_int)) {
            illegalArgument("Cannot store to non int index of type " + TypeUtils.toString(index.type));
        }

        if (array.type.componentType().isPrimitive() && !value.type.equals(array.type.componentType())) {
            illegalArgument("Cannot store non " + TypeUtils.toString(array.type.componentType()) + " value " + TypeUtils.toString(value.type) + " to array of type " + TypeUtils.toString(array.type));
        }

        this.arrayType = array.type;

        use(array, new ArgumentUse(0));
        use(index, new ArgumentUse(1));
        use(value, new ArgumentUse(2));
    }

    @Override
    public String debugDescription() {
        return "ArrayStore : " + TypeUtils.toString(arrayType);
    }
}
