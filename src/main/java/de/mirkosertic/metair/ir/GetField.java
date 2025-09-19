package de.mirkosertic.metair.ir;

public class GetField extends Value {

    public final IRType.MetaClass owner;
    public final String fieldName;

    GetField(final IRType.MetaClass owner, final IRType.MetaClass fieldType, final String fieldName, final Value source) {
        super(fieldType);

        if (source.isPrimitive() || source.isArray()) {
            illegalArgument("Cannot get field " + fieldName + " from non object source " + TypeUtils.toString(source.type));
        }

        this.owner = owner;
        this.fieldName = fieldName;

        use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetField : " + fieldName + " : " + TypeUtils.toString(type);
    }
}
