package de.mirkosertic.metair.ir;

public class StringConstant extends ConstantValue {

    public final String value;

    StringConstant(final String value) {
        super(IRType.CD_String);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "String : " + value;
    }
}
