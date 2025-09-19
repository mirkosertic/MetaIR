package de.mirkosertic.metair.ir;

public class PrimitiveFloat extends PrimitiveValue {

    public final float value;

    PrimitiveFloat(final float value) {
        super(IRType.CD_float);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "float " + value;
    }
}
