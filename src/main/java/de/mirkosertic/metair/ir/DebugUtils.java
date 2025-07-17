package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

public final class DebugUtils {

    private DebugUtils() {
    }

    public static String toString(ClassDesc classDesc) {
        return classDesc.displayName();
    }

    public static String toString(MethodTypeDesc methodTypeDesc) {
        return methodTypeDesc.displayDescriptor();
    }

}
