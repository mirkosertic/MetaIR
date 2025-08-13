package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandleInfo;

public final class TypeUtils {

    public static boolean isCategory2(final ConstantDesc desc) {
        if (desc instanceof ClassDesc) {
            return ConstantDescs.CD_long.equals(desc) || ConstantDescs.CD_double.equals(desc);
        }
        return false;
    }

    public static String toString(final ConstantDesc desc) {
        return switch (desc) {
            case final ClassDesc classDesc -> toString(classDesc);
            case final MethodTypeDesc methodDesc -> toString(methodDesc);
            case final MethodHandleDesc mh -> toString(mh);
            case null, default -> throw new IllegalArgumentException("Unsupported type " + desc);
        };
    }

    public static String toString(final ClassDesc classDesc) {
        return classDesc.displayName();
    }

    public static String toString(final MethodTypeDesc methodTypeDesc) {
        return methodTypeDesc.displayDescriptor();
    }

    public static String toString(final MethodHandleDesc methodHandleDesc) {
        if (methodHandleDesc instanceof final DirectMethodHandleDesc dmh) {
            return MethodHandleInfo.referenceKindToString(dmh.refKind()) + " : " + TypeUtils.toString(dmh.owner()) + "." + dmh.methodName() + " " + TypeUtils.toString(dmh.invocationType());
        }
        return methodHandleDesc.toString();
    }

    public static ConstantDesc jvmInternalTypeOf(final ConstantDesc type) {
        if (ConstantDescs.CD_byte.equals(type)) {
            return ConstantDescs.CD_int;
        }
        if (ConstantDescs.CD_char.equals(type)) {
            return ConstantDescs.CD_int;
        }
        if (ConstantDescs.CD_short.equals(type)) {
            return ConstantDescs.CD_int;
        }
        if (ConstantDescs.CD_boolean.equals(type)) {
            return ConstantDescs.CD_int;
        }

        return type;
    }
}
