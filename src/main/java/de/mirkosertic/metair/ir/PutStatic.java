package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

public class PutStatic extends Node {

    public final String fieldName;
    public final ClassDesc fieldType;

    public final Value arg0;
    public final Value arg1;

    PutStatic(final RuntimeclassReference target, final String fieldName, final ClassDesc fieldType, final Value value) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;

        if (fieldType.equals(ConstantDescs.CD_boolean)) {
            if (!value.type.equals(ConstantDescs.CD_boolean) && !value.type.equals(ConstantDescs.CD_int)) {
                illegalArgument("Cannot put value of type " + TypeUtils.toString(value.type) + " in field " + fieldName + " of type " + TypeUtils.toString(fieldType));
            }
        } else {
            if (fieldType.isPrimitive() && !value.type.equals(fieldType)) {
                illegalArgument("Cannot put value of type " + TypeUtils.toString(value.type) + " in field " + fieldName + " of type " + TypeUtils.toString(fieldType));
            }
        }


        this.arg0 = use(target, new ArgumentUse(0));
        this.arg1 = use(value, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "PutStaticField : " + fieldName + " : " + TypeUtils.toString(fieldType);
    }
}
