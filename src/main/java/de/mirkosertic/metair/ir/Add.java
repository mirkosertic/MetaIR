package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class Add extends Value {

    Add(final Type type, final Value arg1, final Value arg2) {
        super(type);
        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "Add : " + type;
    }
}
