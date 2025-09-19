package de.mirkosertic.metair.ir;

public class ArrayStore extends Node {

    public final IRType.MetaClass arrayType;

    ArrayStore(final Value array, final Value index, final Value value) {
        if (!array.isArray()) {
            illegalArgument("Cannot store to non array of type " + TypeUtils.toString(array.type));
        }

        arrayType = (IRType.MetaClass) array.type;

        if (!index.type.equals(IRType.CD_int)) {
            illegalArgument("Cannot store to non int index of type " + TypeUtils.toString(index.type));
        }

        if (arrayType.componentType().isPrimitive() && !value.type.equals(arrayType.componentType())) {
            illegalArgument("Cannot store non " + TypeUtils.toString(arrayType.componentType()) + " value " + TypeUtils.toString(value.type) + " to array of type " + TypeUtils.toString(array.type));
        }

        use(array, new ArgumentUse(0));
        use(index, new ArgumentUse(1));
        use(value, new ArgumentUse(2));
    }

    @Override
    public String debugDescription() {
        return "ArrayStore : " + TypeUtils.toString(arrayType);
    }
}
