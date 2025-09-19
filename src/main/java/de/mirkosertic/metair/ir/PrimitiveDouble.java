package de.mirkosertic.metair.ir;

public class PrimitiveDouble extends PrimitiveValue {

    public final double value;

    PrimitiveDouble(final double value) {
        super(IRType.CD_double);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "double " + value;
    }
}
