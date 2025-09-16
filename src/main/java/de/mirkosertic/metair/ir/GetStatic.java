package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class GetStatic extends Value {

    public final String fieldName;

    GetStatic(final RuntimeclassReference source, final String fieldName, final ClassDesc fieldType) {
        super(fieldType);
        this.fieldName = fieldName;

        use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetStaticField : " + fieldName + " : " + TypeUtils.toString(type);
    }
}
