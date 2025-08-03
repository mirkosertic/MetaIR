package de.mirkosertic.metair.ir.test;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

import java.lang.reflect.Method;

class MethodTestDescriptor extends AbstractTestDescriptor {

    private final Class<?> testClass;
    private final Method testMethod;

    public MethodTestDescriptor(final Class<?> testClass, final Method testMethod, final TestDescriptor parent) {
        super( //
                parent.getUniqueId().append("method", testMethod.getName()), //
                testMethod.getName(), //
                MethodSource.from(testMethod)
        );
        this.testClass = testClass;
        this.testMethod = testMethod;
        setParent(parent);
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public Method getTestMethod() {
        return testMethod;
    }

    @Override
    public Type getType() {
        return Type.TEST;
    }
}
