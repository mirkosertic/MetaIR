package de.mirkosertic.metair.ir;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

public class ResolvedClass {

    private final ResolverContext resolverContext;
    private ClassFile classFile;
    private ClassModel classModel;
    private boolean loaded;

    public ResolvedClass(final ResolverContext resolverContext) {
        this.loaded = false;
        this.resolverContext = resolverContext;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void loaded(final ClassFile classFile, final ClassModel classModel) {
        this.classFile = classFile;
        this.classModel = classModel;
        this.loaded = true;
    }

    public ClassModel classModel() {
        return classModel;
    }
}
