package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class StringConstant extends Value {

    public final String value;

    StringConstant(final String value) {
        super(Type.getType(String.class));
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "String : " + value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
