package de.mirkosertic.metair.ir;

public class Throw extends Node {

    public final Value arg0;

    Throw(final Value object) {

        if (object.isPrimitive()) {
            illegalArgument("Cannot throw a primitive value of type " + TypeUtils.toString(object.type));
        }

        this.arg0 = use(object, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
