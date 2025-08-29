package de.mirkosertic.metair.ir;


import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.HashMap;
import java.util.Map;

public class Method extends TupleNode {

    private final Map<ClassDesc, RuntimeclassReference> runtimeclassReferences;
    private final Map<MethodTypeDesc, MethodType> methodtypeReferences;
    private final Map<MethodHandleDesc, MethodHandle> methodHandles;

    Method() {
        this.runtimeclassReferences = new HashMap<>();
        this.methodtypeReferences = new HashMap<>();
        this.methodHandles = new HashMap<>();

        registerAs("default", this);
    }

    public RuntimeclassReference defineRuntimeclassReference(final ClassDesc type) {
        return runtimeclassReferences.computeIfAbsent(type, key -> {
            final RuntimeclassReference r = new RuntimeclassReference(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public MethodType defineMethodType(final MethodTypeDesc type) {
        return methodtypeReferences.computeIfAbsent(type, key -> {
            final MethodType r = new MethodType(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public MethodHandle defineMethodHandle(final MethodHandleDesc type) {
        return methodHandles.computeIfAbsent(type, key -> {
            final MethodHandle r = new MethodHandle(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public ExtractThisRefProjection defineThisRef(final ClassDesc type) {
        final ExtractThisRefProjection n = new ExtractThisRefProjection(type, this);
        registerAs(n.name(), n);
        return n;
    }

    public ExtractMethodArgProjection defineMethodArgument(final ConstantDesc type, final int index) {
        final ExtractMethodArgProjection n = new ExtractMethodArgProjection(type, this, index);
        registerAs(n.name(), n);
        return n;
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
