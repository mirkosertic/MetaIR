package de.mirkosertic.metair.ir;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Method extends TupleNode {

    private final Map<IRType.MethodType, MethodType> methodtypeReferences;
    private final Map<IRType.MethodHandle, MethodHandle> methodHandles;

    public final List<Value> methodArguments;

    Method() {
        this.methodArguments = new ArrayList<>();
        this.methodtypeReferences = new HashMap<>();
        this.methodHandles = new HashMap<>();

        registerAs("default", this);
    }

    public MethodType defineMethodType(final IRType.MethodType type) {
        return methodtypeReferences.computeIfAbsent(type, key -> {
            final MethodType r = new MethodType(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public MethodHandle defineMethodHandle(final IRType.MethodHandle type) {
        return methodHandles.computeIfAbsent(type, key -> {
            final MethodHandle r = new MethodHandle(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public ExtractThisRefProjection defineThisRef(final IRType.MetaClass type) {
        final ExtractThisRefProjection n = new ExtractThisRefProjection(type, this);
        registerAs(n.name(), n);

        methodArguments.add(n);

        return n;
    }

    public ExtractMethodArgProjection defineMethodArgument(final IRType.MetaClass type, final int index) {
        final ExtractMethodArgProjection n = new ExtractMethodArgProjection(type, this, index);
        registerAs(n.name(), n);

        methodArguments.add(n);

        return n;
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
