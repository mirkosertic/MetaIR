package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class PutStatic extends Node {

    public final String fieldName;
    public final ClassDesc fieldType;

    PutStatic(final RuntimeclassReference target, final String fieldName, final ClassDesc fieldType, final Value value) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;

        if (fieldType.isPrimitive() && !value.type.equals(fieldType)) {
            illegalArgument("Cannot put value of type " + TypeUtils.toString(value.type) + " in field " + fieldName + " of type " + TypeUtils.toString(fieldType));
        }

        use(target, new ArgumentUse(0));
        use(value, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "PutStaticField : " + fieldName + " : " + TypeUtils.toString(fieldType);
    }
}
