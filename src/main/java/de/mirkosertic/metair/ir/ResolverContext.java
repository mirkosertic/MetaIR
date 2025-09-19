package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class ResolverContext {

    public IRType.MetaClass resolveType(final ClassDesc desc) {
        return IRType.MetaClass.of(desc);
    }

    public IRType.MetaClass resolveType(final Class cls) {
        return IRType.MetaClass.of(cls);
    }
}
