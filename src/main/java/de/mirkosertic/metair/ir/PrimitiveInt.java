package de.mirkosertic.metair.ir;

public class PrimitiveInt extends PrimitiveValue {

    public final int value;

    public PrimitiveInt(final int value) {
        super(IRType.CD_int);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "int " + value;
    }
}
