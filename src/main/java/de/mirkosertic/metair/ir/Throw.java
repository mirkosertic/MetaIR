package de.mirkosertic.metair.ir;

public class Throw extends Node {

    Throw(final Value object) {

        if (object.isPrimitive()) {
            illegalArgument("Cannot throw a primitive value of type " + TypeUtils.toString(object.type));
        }

        use(object, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
