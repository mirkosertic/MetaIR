package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class PutField extends Node {

    public final ClassDesc owner;
    public final String fieldName;
    public final ClassDesc fieldType;

    PutField(final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Value target, final Value value) {

        if (target.type.isPrimitive() || target.type.isArray()) {
            illegalArgument("Cannot put field " + fieldName + " on non object target " + TypeUtils.toString(target.type));
        }
        if (fieldType.isPrimitive() && !value.type.equals(fieldType)) {
            illegalArgument("Cannot put value of type " + TypeUtils.toString(value.type) + " in field " + fieldName + " of type " + TypeUtils.toString(fieldType));
        }

        this.owner = owner;
        this.fieldName = fieldName;
        this.fieldType = fieldType;

        use(target, new ArgumentUse(0));
        use(value, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "PutField : " + fieldName + " : " + TypeUtils.toString(fieldType);
    }
}
