package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

public abstract class IRType<T extends ConstantDesc> {

    public final static MetaClass CD_byte = new MetaClass(ConstantDescs.CD_byte);
    public final static MetaClass CD_char = new MetaClass(ConstantDescs.CD_char);
    public final static MetaClass CD_short = new MetaClass(ConstantDescs.CD_short);
    public final static MetaClass CD_boolean = new MetaClass(ConstantDescs.CD_boolean);
    public final static MetaClass CD_int = new MetaClass(ConstantDescs.CD_int);
    public final static MetaClass CD_float = new MetaClass(ConstantDescs.CD_float);
    public final static MetaClass CD_long = new MetaClass(ConstantDescs.CD_long);
    public final static MetaClass CD_double = new MetaClass(ConstantDescs.CD_double);

    public final static MetaClass CD_String = new MetaClass(ConstantDescs.CD_String);
    public final static MetaClass CD_Object = new MetaClass(ConstantDescs.CD_Object);

    public final static MetaClass CD_void = new MetaClass(ConstantDescs.CD_void);

    protected final T type;

    public IRType(final T type) {
        this.type = type;
    }

    public T type() {
        return type;
    }

    public abstract boolean isArray();

    public abstract boolean isPrimitive();

    public abstract MetaClass arrayType();

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof IRType<?>) {
            return type.equals(((IRType<?>) obj).type);
        }
        return false;
    }

    public static class MetaClass extends IRType<ClassDesc> {

        public static MetaClass of(final ClassDesc type) {
            return new MetaClass(type);
        }

        public static MetaClass of(final Class<?> type) {
            return new MetaClass(ClassDesc.of(type.getName()));
        }

        private MetaClass(final ClassDesc type) {
            super(type);
        }

        public boolean isArray() {
            return type.isArray();
        }

        public boolean isPrimitive() {
            return type.isPrimitive();
        }

        public MetaClass componentType() {
            return new MetaClass(type.componentType());
        }

        public MetaClass arrayType() {
            return new MetaClass(type.arrayType());
        }
    }

    public static class MethodType extends IRType<MethodTypeDesc> {

        private final MetaClass returnType;
        private final List<MetaClass> parameterTypes;

        public MethodType(final MethodTypeDesc type, final MetaClass returnType, final List<MetaClass> parameterTypes) {
            super(type);
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public MetaClass arrayType() {
            return null;
        }

        public MetaClass returnType() {
            return returnType;
        }

        public int parameterCount() {
            return parameterTypes.size();
        }

        public MetaClass parameterType(final int i) {
            return parameterTypes.get(i);
        }
    }

    public static class MethodHandle extends IRType<MethodHandleDesc> {

        public MethodHandle(final MethodHandleDesc type) {
            super(type);
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public MetaClass arrayType() {
            return null;
        }
    }
}
