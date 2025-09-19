package de.mirkosertic.metair.ir;

public class PutField extends Node {

    public final IRType.MetaClass owner;
    public final String fieldName;
    public final IRType.MetaClass fieldType;

    PutField(final IRType.MetaClass owner, final IRType.MetaClass fieldType, final String fieldName, final Value target, final Value value) {

        if (target.isPrimitive() || target.isArray()) {
            illegalArgument("Cannot put field " + fieldName + " on non object target " + TypeUtils.toString(target.type));
        }

        if (fieldType.equals(IRType.CD_boolean)) {
            if (!value.type.equals(IRType.CD_boolean) && !value.type.equals(IRType.CD_int)) {
                illegalArgument("Cannot put value of type " + TypeUtils.toString(value.type) + " in field " + fieldName + " of type " + TypeUtils.toString(fieldType));
            }
        } else {
            if (fieldType.isPrimitive() && !value.type.equals(fieldType)) {
                illegalArgument("Cannot put value of type " + TypeUtils.toString(value.type) + " in field " + fieldName + " of type " + TypeUtils.toString(fieldType));
            }
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
