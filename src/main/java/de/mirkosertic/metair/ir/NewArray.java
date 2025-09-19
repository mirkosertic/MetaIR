package de.mirkosertic.metair.ir;

public class NewArray extends Value {

    NewArray(final IRType.MetaClass componentType, final Value length) {
        super(componentType.arrayType());

        if (!IRType.CD_int.equals(length.type)) {
            illegalArgument("Array length must be int, but was " + TypeUtils.toString(length.type));
        }

        use(length, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "NewArray : " + TypeUtils.toString(type);
    }
}
