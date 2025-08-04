package de.mirkosertic.metair.ir.test;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

class MethodTestDescriptor extends AbstractTestDescriptor {

    private final Class<?> testClass;
    private final String methodName;

    public MethodTestDescriptor(final Class<?> testClass, final String methodName, final TestDescriptor parent) {
        super( //
                parent.getUniqueId().append("method", methodName), //
                methodName, //
                MethodSource.from(testClass.getName(), methodName)
        );
        this.testClass = testClass;
        this.methodName = methodName;
        setParent(parent);
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public Type getType() {
        return Type.TEST;
    }
}
