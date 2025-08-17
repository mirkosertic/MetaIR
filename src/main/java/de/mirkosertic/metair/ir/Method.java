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
    private final Map<Object, Value> constants;
    private Null nullref;

    Method() {
        this.runtimeclassReferences = new HashMap<>();
        this.constants = new HashMap<>();
        this.methodtypeReferences = new HashMap<>();
        this.methodHandles = new HashMap<>();
        this.nullref = null;

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

    public Null defineNullReference() {
        if (nullref == null) {
            nullref = new Null();
            nullref.use(this, DefinedByUse.INSTANCE);
        }
        return nullref;
    }

    public StringConstant defineStringConstant(final String value) {
        return (StringConstant) constants.computeIfAbsent(value, key -> {
            final StringConstant r = new StringConstant(value);
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

    public PrimitiveInt definePrimitiveInt(final int value) {
        return (PrimitiveInt) constants.computeIfAbsent(value, key -> {
            final PrimitiveInt v = new PrimitiveInt(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveLong definePrimitiveLong(final long value) {
        return (PrimitiveLong) constants.computeIfAbsent(value, key -> {
            final PrimitiveLong v = new PrimitiveLong(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveFloat definePrimitiveFloat(final float value) {
        return (PrimitiveFloat) constants.computeIfAbsent(value, key -> {
            final PrimitiveFloat v = new PrimitiveFloat(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveDouble definePrimitiveDouble(final double value) {
        return (PrimitiveDouble) constants.computeIfAbsent(value, key -> {
            final PrimitiveDouble v = new PrimitiveDouble(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
