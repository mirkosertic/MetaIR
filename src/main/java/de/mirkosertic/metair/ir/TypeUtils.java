package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;

public final class TypeUtils {

    public static boolean isCategory2(final ClassDesc desc) {
        return ConstantDescs.CD_long.equals(desc) || ConstantDescs.CD_double.equals(desc);
    }

    public static String toString(final ClassDesc classDesc) {
        return classDesc.displayName();
    }

    public static String toString(final MethodTypeDesc methodTypeDesc) {
        return methodTypeDesc.displayDescriptor();
    }
}
