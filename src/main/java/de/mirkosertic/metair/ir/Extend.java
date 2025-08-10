
package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Extend extends Value {

    public enum ExtendType {
        SIGN, ZERO
    }

    public final ExtendType extendType;

    Extend(final ClassDesc targetType, final ExtendType extendType, final Value value) {
        super(targetType);

        this.extendType = extendType;

        use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "Extend : " +extendType + " " + TypeUtils.toString(type);
    }
}
