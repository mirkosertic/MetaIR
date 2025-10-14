package de.mirkosertic.metair.ir;

import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResolvedClass {

    private final ResolverContext resolverContext;
    private ClassModel classModel;
    private boolean loaded;
    private ResolvedClass superClass;
    private final List<ResolvedClass> interfaces;
    private IRType.MetaClass thisType;
    private final Map<String, ResolvedField> fields;

    public ResolvedClass(final ResolverContext resolverContext) {
        this.loaded = false;
        this.resolverContext = resolverContext;
        this.interfaces = new ArrayList<>();
        this.superClass = null;
        this.fields = new HashMap<>();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<ResolvedField> resolvedFields() {
        return new ArrayList<>(fields.values());
    }

    public boolean hasInterface(final ClassDesc interfaceType) {
        for (final ResolvedClass i : interfaces) {
            if (i.thisType().type().equals(interfaceType)) {
                return true;
            }
        }
        return false;
    }

    public void loaded(final ClassModel classModel, final ResolvedClass superClass, final List<ResolvedClass> interfaces) {
        this.classModel = classModel;
        this.loaded = true;
        this.superClass = superClass;
        this.interfaces.addAll(interfaces);

        this.thisType = IRType.MetaClass.of(classModel.thisClass().asSymbol());

        for (final MethodModel methodModel : classModel.methods()) {
            if (!methodModel.flags().has(AccessFlag.ABSTRACT) && !methodModel.flags().has(AccessFlag.NATIVE) && "<clinit>".equals(methodModel.methodName().stringValue())) {
                // We need to resolve a static class initializers
                resolveMethod(methodModel);
            }
        }
    }

    public ResolvedMethod resolveMethodForSpecialInvocation(final String methodName, final MethodTypeDesc methodTypeDesc) {
        for (final MethodModel methodModel : classModel.methods()) {
            if (methodModel.methodName().stringValue().equals(methodName) && methodModel.methodTypeSymbol().equals(methodTypeDesc)) {
                return resolveMethod(methodModel);
            }
        }
        throw new IllegalArgumentException("Cannot find method " + methodName + " with type " + TypeUtils.toString(methodTypeDesc));
    }

    public ResolvedMethod resolveMethodForStaticInvocation(final String methodName, final MethodTypeDesc methodTypeDesc) {
        for (final MethodModel methodModel : classModel.methods()) {
            if (methodModel.methodName().stringValue().equals(methodName) && methodModel.methodTypeSymbol().equals(methodTypeDesc)) {
                return resolveMethod(methodModel);
            }
        }
        throw new IllegalArgumentException("Cannot find method " + methodName + " with type " + TypeUtils.toString(methodTypeDesc));
    }

    public ResolvedMethod resolveMethod(final MethodModel methodModel) {
        return new ResolvedMethod(resolverContext, this, methodModel);
    }

    public ClassModel classModel() {
        return classModel;
    }

    public IRType.MetaClass thisType() {
        return thisType;
    }

    public ResolvedField resolveStaticField(final String fieldName) {
        return fields.computeIfAbsent(fieldName, key -> {
            for (final FieldModel fieldModel : classModel.fields()) {
                if (fieldModel.flags().has(AccessFlag.STATIC) && fieldName.equals(fieldModel.fieldName().stringValue())) {
                    final IRType.MetaClass type = resolverContext.resolveType(fieldModel.fieldTypeSymbol());
                    return new ResolvedField(ResolvedClass.this, key, type, fieldModel);
                }
            }
            // TODO: Check in class hierarchy
            if (superClass != null) {
                return superClass.resolveStaticField(fieldName);
            }
            throw new IllegalArgumentException("Cannot find static field " + key + " in class " + TypeUtils.toString(ResolvedClass.this.thisType()));
        });
    }

    public ResolvedField resolveMemberField(final String fieldName) {
        return fields.computeIfAbsent(fieldName, key -> {
            for (final FieldModel fieldModel : classModel.fields()) {
                if (!fieldModel.flags().has(AccessFlag.STATIC) && fieldName.equals(fieldModel.fieldName().stringValue())) {
                    final IRType.MetaClass type = resolverContext.resolveType(fieldModel.fieldTypeSymbol());
                    return new ResolvedField(ResolvedClass.this, key, type, fieldModel);
                }
            }
            // TODO: Check in class hierarchy
            if (superClass != null) {
                return superClass.resolveMemberField(fieldName);
            }
            throw new IllegalArgumentException("Cannot find  field " + key + " in class " + TypeUtils.toString(ResolvedClass.this.thisType()));
        });
    }
}
