package de.mirkosertic.metair.ir;

public class MonitorEnter extends Node {

    MonitorEnter(final Value object) {

        if (object.type.isPrimitive()) {
            illegalArgument("Expecting non primitive type for monitorenter on stack, got " + TypeUtils.toString(object.type));
        }

        use(object, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
