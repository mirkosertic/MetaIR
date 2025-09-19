package de.mirkosertic.metair.ir;

public class ArrayLoad extends Value {

    ArrayLoad(final IRType.MetaClass arrayType, final Value array, final Value index) {
        super(arrayType.componentType());

        if (!array.isArray()) {
            illegalArgument("Cannot store to non array of type " + TypeUtils.toString(array.type));
        }

        if (!index.type.equals(IRType.CD_int)) {
            illegalArgument("Cannot store to non int index of type " + TypeUtils.toString(index.type));
        }

        use(array, new ArgumentUse(0));
        use(index, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "ArrayLoad : " + TypeUtils.toString(type);
    }
}
