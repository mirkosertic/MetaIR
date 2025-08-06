package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class RuntimeclassReference extends Value {

    RuntimeclassReference(final ClassDesc type) {
        super(type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "Class " + TypeUtils.toString(type);
    }
}
