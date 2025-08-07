package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class GetField extends Value {

    public final ClassDesc owner;
    public final String fieldName;

    GetField(final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Value source) {
        super(fieldType);
        this.owner = owner;
        this.fieldName = fieldName;

        use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetField : " + fieldName + " : " + TypeUtils.toString(type);
    }
}
