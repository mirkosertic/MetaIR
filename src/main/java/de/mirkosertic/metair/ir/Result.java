package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class Result extends Value {

    Result(final Type type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "Result : " + type;
    }
}
