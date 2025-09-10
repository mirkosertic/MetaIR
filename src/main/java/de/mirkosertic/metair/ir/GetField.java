package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class GetField extends Value {

    public final ClassDesc owner;
    public final Value arg0;
    public final String fieldName;

    GetField(final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Value source) {
        super(fieldType);

        if (source.isPrimitive() || source.isArray()) {
            illegalArgument("Cannot get field " + fieldName + " from non object source " + TypeUtils.toString(source.type));
        }

        this.owner = owner;
        this.fieldName = fieldName;

        this.arg0 = use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetField : " + fieldName + " : " + TypeUtils.toString(type);
    }
}
