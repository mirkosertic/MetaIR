package de.mirkosertic.metair.ir;

public class Null extends ConstantValue {

    Null() {
        super(IRType.CD_Object);
    }

    @Override
    public String debugDescription() {
        return "null";
    }
}
