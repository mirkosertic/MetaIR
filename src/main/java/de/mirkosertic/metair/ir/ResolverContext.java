package de.mirkosertic.metair.ir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResolverContext {

    private final ClassLoader classLoader;
    private final Map<String, ResolvedClass> resolvedClasses;

    public ResolverContext(final ClassLoader aClassLoader) {
        this.classLoader = aClassLoader;
        this.resolvedClasses = new HashMap<>();
    }

    public ResolverContext() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public int numberOrResolvedClasses() {
        return resolvedClasses.size();
    }

    public ResolvedClass resolveClass(final ClassModel model) {
        final ClassDesc desc = model.thisClass().asSymbol();
        final String className = desc.packageName() + "." + desc.displayName();
        final ResolvedClass result = resolvedClasses.computeIfAbsent(className, key -> new ResolvedClass(this));
        if (!result.isLoaded()) {
            result.loaded(model, null, new ArrayList<>());
        }
        return result;
    }

    public ResolvedClass resolveClass(final String className) {
        try {
            final ResolvedClass resolved = resolvedClasses.computeIfAbsent(className, k -> new ResolvedClass(this));

            if (!resolved.isLoaded()) {
                // Try OS specific naming
                String resourceToSearch = className.replace('.', File.separatorChar) + ".class";
                URL resource = classLoader.getResource(resourceToSearch);
                if (resource == null) {
                    // Try the hard coded unix way
                    resourceToSearch = className.replace('.', '/') + ".class";
                    resource = classLoader.getResource(resourceToSearch);
                    if (resource == null) {
                        throw new IllegalStateException("Cannot find class file for " + className + " as resource " + resourceToSearch);
                    }

                }

                try (final InputStream inputStream = resource.openStream()) {
                    final byte[] data = inputStream.readAllBytes();

                    final ClassFile cf = ClassFile.of();
                    final ClassModel model = cf.parse(data);

                    // Resolve the superclass
                    ResolvedClass superClass = null;
                    if (model.superclass().isPresent()) {
                        superClass = resolveClass(model.superclass().get().name().toString());
                    }

                    // Resolve all implementing interfaces
                    final List<ResolvedClass> interfaces = new ArrayList<>();
                    for (final ClassEntry iface : model.interfaces()) {
                        interfaces.add(resolveClass(iface.name().toString()));
                    }

                    resolved.loaded(model, superClass, interfaces);
                }
            }
            return resolved;
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot resolve class " + className, e);
        }
    }

    public IRType.MetaClass resolveType(final ClassDesc desc) {
        if (desc.isArray()) {
            resolveType(desc.componentType());
        }
        if (!desc.isPrimitive() && !desc.isArray()) {
            final ResolvedClass resolved = resolveClass(desc);

        }
        return IRType.MetaClass.of(desc);
    }

    public IRType.MethodType resolveMethodType(final MethodTypeDesc desc) {
        final IRType.MetaClass returnType = resolveType(desc.returnType());
        final IRType.MetaClass[] parameterTypes = new IRType.MetaClass[desc.parameterArray().length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = resolveType(desc.parameterArray()[i]);
        }
        return new IRType.MethodType(desc, returnType, java.util.List.of(parameterTypes));
    }

    public ResolvedClass resolveClass(final ClassDesc owner) {
        if (owner.isArray()) {
            return resolveClass(Array.class.getName());
        }
        return resolveClass(owner.packageName() + "." + owner.displayName());
    }

    public ResolvedMethod resolveInvokeSpecial(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc) {
        final ResolvedClass resolvedClass = resolveClass(owner);
        // TODO
        //return resolvedClass.resolveMethodForSpecialInvocation(methodName, methodTypeDesc);
        return null;
    }

    public ResolvedMethod resolveInvokeStatic(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc) {
        final ResolvedClass resolvedClass = resolveClass(owner);
        // TODO
        // return resolvedClass.resolveMethodForStaticInvocation(methodName, methodTypeDesc);
        return null;
    }

    public ResolvedMethod resolveInvokeInterface(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc) {
        final ResolvedClass resolvedClass = resolveClass(owner);
        // TODO
        return null;
    }

    public ResolvedMethod resolveInvokeVirtual(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc) {
        final ResolvedClass resolvedClass = resolveClass(owner);
        // TODO
        return null;
    }

    public ResolvedField resolveMemberField(final ClassDesc owner, final String fieldName, final ClassDesc fieldType) {
        final ResolvedClass resolvedClass = resolveClass(owner);
        return resolvedClass.resolveMemberField(fieldName);
    }

    public ResolvedField resolveStaticField(final ClassDesc owner, final String fieldName, final ClassDesc fieldType) {
        final ResolvedClass resolvedClass = resolveClass(owner);
        return resolvedClass.resolveStaticField(fieldName);
    }
}
