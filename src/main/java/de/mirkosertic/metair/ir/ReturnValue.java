package de.mirkosertic.metair.ir;

public class ReturnValue extends Value {

    ReturnValue(final IRType type, final Value value) {
        super(type);

        if (type instanceof final IRType.MetaClass cds) {
            if (cds.isPrimitive() && !value.isPrimitive()) {
                illegalArgument("Expecting type " + TypeUtils.toString(type) + " as value, got " + TypeUtils.toString(value.type));
            }
            if (!cds.isPrimitive() && value.isPrimitive()) {
                illegalArgument("Expecting type " + TypeUtils.toString(type) + " as value, got " + TypeUtils.toString(value.type));
            }
        }

        use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "ReturnValue : " + TypeUtils.toString(type);
    }
}
