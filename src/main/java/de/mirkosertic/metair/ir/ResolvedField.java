package de.mirkosertic.metair.ir;

import java.lang.classfile.FieldModel;

public class ResolvedField {

    private final ResolvedClass owner;
    private final String fieldName;
    private final IRType.MetaClass type;
    private final FieldModel fieldModel;

    public ResolvedField(final ResolvedClass owner, final String fieldName, final IRType.MetaClass type, final FieldModel fieldModel) {
        this.owner = owner;
        this.fieldName = fieldName;
        this.type = type;
        this.fieldModel = fieldModel;
    }
}
