package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

public class ResolverContext {

    public IRType.MetaClass resolveType(final ClassDesc desc) {
        return IRType.MetaClass.of(desc);
    }

    public IRType.MetaClass resolveType(final Class cls) {
        return IRType.MetaClass.of(cls);
    }

    public IRType.MethodType resolveMethodType(final MethodTypeDesc desc) {
        final IRType.MetaClass returnType = resolveType(desc.returnType());
        final IRType.MetaClass[] parameterTypes = new IRType.MetaClass[desc.parameterArray().length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = resolveType(desc.parameterArray()[i]);
        }
        return new IRType.MethodType(desc, returnType, java.util.List.of(parameterTypes));
    }
}
