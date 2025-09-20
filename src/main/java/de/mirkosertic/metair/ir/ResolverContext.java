package de.mirkosertic.metair.ir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ResolverContext {

    private final ClassLoader classLoader;
    private final Map<String, ResolvedClass> resolvedClasses;

    public ResolverContext(final ClassLoader aClassLoader) {
        this.classLoader = aClassLoader;
        this.resolvedClasses = new HashMap<>();
    }

    public ResolverContext() {
        this(ResolverContext.class.getClassLoader());
    }

    public ResolvedClass resolveClass(final String className) throws IOException {
        final ResolvedClass resolved = resolvedClasses.computeIfAbsent(className, k -> new ResolvedClass(this));

        if (!resolved.isLoaded()) {
            final URL resource = classLoader.getResource(className.replace('.', File.separatorChar) + ".class");
            if (resource == null) {
                throw new IllegalStateException("Cannot find class file for " + className);
            }

            try (final InputStream inputStream = resource.openStream()) {
                final byte[] data = inputStream.readAllBytes();

                final ClassFile cf = ClassFile.of();
                final ClassModel model = cf.parse(data);

                resolved.loaded(cf, model);
            }
        }
        return resolved;
    }

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
