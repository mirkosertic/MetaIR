package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandleInfo;

public final class TypeUtils {

    public static boolean isCategory2(final IRType desc) {
        if (desc instanceof IRType.MetaClass) {
            return IRType.CD_long.equals(desc) || IRType.CD_double.equals(desc);
        }
        return false;
    }

    public static String toString(final IRType type) {
        if (type instanceof final IRType.MetaClass cls) {
            return toString(cls.type);
        }
        if (type instanceof final IRType.MethodType cls) {
            return toString(cls.type);
        }
        if (type instanceof final IRType.MethodHandle mh) {
            return toString(mh.type);
        }
        return toString(type.type());
    }

    public static String toString(final ConstantDesc desc) {
        return switch (desc) {
            case final MethodTypeDesc methodDesc -> toString(methodDesc);
            case final MethodHandleDesc mh -> toString(mh);
            case null, default -> throw new IllegalArgumentException("Unsupported type " + desc);
        };
    }

    private static String toString(final ClassDesc classDesc) {
        return classDesc.displayName();
    }

    private static String toString(final MethodTypeDesc methodTypeDesc) {
        return methodTypeDesc.displayDescriptor();
    }

    private static String toString(final MethodHandleDesc methodHandleDesc) {
        if (methodHandleDesc instanceof final DirectMethodHandleDesc dmh) {
            return MethodHandleInfo.referenceKindToString(dmh.refKind()) + " : " + TypeUtils.toString(dmh.owner()) + "." + dmh.methodName() + " " + TypeUtils.toString(dmh.invocationType());
        }
        return methodHandleDesc.toString();
    }

    public static IRType jvmInternalTypeOf(final IRType type) {

        if (type instanceof IRType.MetaClass) {
            if (IRType.CD_byte.equals(type)) {
                return IRType.CD_int;
            }
            if (IRType.CD_char.equals(type)) {
                return IRType.CD_int;
            }
            if (IRType.CD_short.equals(type)) {
                return IRType.CD_int;
            }
            if (IRType.CD_boolean.equals(type)) {
                return IRType.CD_int;
            }
        }

        return type;
    }
}
