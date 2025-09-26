package de.mirkosertic.metair.ir;

import java.lang.classfile.MethodModel;

public class ResolvedMethod {

    private final ResolverContext resolverContext;
    private final ResolvedClass owner;
    private final MethodModel methodModel;
    private MethodAnalyzer analyzer;

    public ResolvedMethod(final ResolverContext resolverContext, final ResolvedClass owner, final MethodModel methodModel) {
        this.resolverContext = resolverContext;
        this.owner = owner;
        this.methodModel = methodModel;
    }

    public MethodModel methodModel() {
        return methodModel;
    }

    public ResolvedClass thisClass() {
        return owner;
    }

    public MethodAnalyzer analyze() {
        if (analyzer == null) {
            analyzer = new MethodAnalyzer(resolverContext, owner.thisType(), methodModel);
        }
        return analyzer;
    }
}
