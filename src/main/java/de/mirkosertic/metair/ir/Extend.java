
package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Extend extends Value {

    public enum ExtendType {
        SIGN, ZERO
    }

    public final ExtendType extendType;
    public final Value arg0;

    Extend(final ClassDesc targetType, final ExtendType extendType, final Value value) {
        super(targetType);

        this.extendType = extendType;

        this.arg0 = use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "Extend : " +extendType + " " + TypeUtils.toString(type);
    }
}
