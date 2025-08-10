package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

public abstract class Invocation extends Value {

    public final ClassDesc ownerType;
    public final Value target;
    public final String name;
    public final MethodTypeDesc typeDesc;

    Invocation(final ClassDesc ownerType, final Value target, final String name, final MethodTypeDesc methodTypeDesc, final List<Value> arguments) {
        super(TypeUtils.jvmInternalTypeOf(methodTypeDesc.returnType()));

        if (target.type.isPrimitive()) {
            illegalArgument("Cannot invoke a method on a primitive value");
        }

        if (arguments.size() != methodTypeDesc.parameterCount()) {
            illegalArgument("Wrong number of arguments for method " + name + " : " + methodTypeDesc.parameterCount() + " expected, but got " + arguments.size());
        }

        for (int i = 0; i < arguments.size(); i++) {
            if (methodTypeDesc.parameterType(i).isPrimitive() && !TypeUtils.jvmInternalTypeOf(arguments.get(i).type).equals(TypeUtils.jvmInternalTypeOf(methodTypeDesc.parameterType(i)))) {
                // TODO: Convert byte, char, short, boolean rsp. type check it
                // TODO: Floating point conversion if required...
                illegalArgument("Parameter " + i + " of method " + name + " is a " + TypeUtils.toString(methodTypeDesc.parameterType(i)) + " type, but got " + TypeUtils.toString(arguments.get(i).type));
            }
        }

        this.ownerType = ownerType;
        this.target = target;
        this.name = name;
        this.typeDesc = methodTypeDesc;

        int index = 0;
        use(target, new ArgumentUse(index++));
        for (final Value v : arguments) {
            use(v, new ArgumentUse(index++));
        }
    }
}
