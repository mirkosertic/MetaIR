
package de.mirkosertic.metair.ir;

public class Extend extends Value {

    public enum ExtendType {
        SIGN, ZERO
    }

    public final ExtendType extendType;

    Extend(final IRType.MetaClass targetType, final ExtendType extendType, final Value value) {
        super(targetType);

        this.extendType = extendType;

        use(value, new ArgumentUse(0));
    }

    @Override
    public boolean sideeffectFree() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "Extend : " +extendType + " " + TypeUtils.toString(type);
    }
}
