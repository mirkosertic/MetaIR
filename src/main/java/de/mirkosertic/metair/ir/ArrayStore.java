package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

public class ArrayStore extends Node {

    public final ClassDesc arrayType;
    public final Value arg0;
    public final Value arg1;
    public final Value arg2;

    ArrayStore(final Value array, final Value index, final Value value) {
        if (!array.isArray()) {
            illegalArgument("Cannot store to non array of type " + TypeUtils.toString(array.type));
        }

        arrayType = (ClassDesc) array.type;

        if (!index.type.equals(ConstantDescs.CD_int)) {
            illegalArgument("Cannot store to non int index of type " + TypeUtils.toString(index.type));
        }

        if (arrayType.componentType().isPrimitive() && !value.type.equals(arrayType.componentType())) {
            illegalArgument("Cannot store non " + TypeUtils.toString(arrayType.componentType()) + " value " + TypeUtils.toString(value.type) + " to array of type " + TypeUtils.toString(array.type));
        }

        this.arg0 = use(array, new ArgumentUse(0));
        this.arg1 = use(index, new ArgumentUse(1));
        this.arg2 = use(value, new ArgumentUse(2));
    }

    @Override
    public String debugDescription() {
        return "ArrayStore : " + TypeUtils.toString(arrayType);
    }
}
