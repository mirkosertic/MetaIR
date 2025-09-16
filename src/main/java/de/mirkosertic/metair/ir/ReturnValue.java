package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;

public class ReturnValue extends Value {

    ReturnValue(final ConstantDesc type, final Value value) {
        super(type);

        if (type instanceof final ClassDesc cds) {
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
