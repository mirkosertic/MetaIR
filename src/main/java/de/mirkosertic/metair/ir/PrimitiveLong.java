package de.mirkosertic.metair.ir;

public class PrimitiveLong extends PrimitiveValue {

    public final long value;

    PrimitiveLong(final long value) {
        super(IRType.CD_long);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "long " + value;
    }
}
