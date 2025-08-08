package de.mirkosertic.metair.ir;

public class MonitorExit extends Node {

    MonitorExit(final Value object) {

        if (object.type.isPrimitive()) {
            illegalArgument("Expecting non primitive type for monitorexit on stack, got " + TypeUtils.toString(object.type));
        }

        use(object, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
