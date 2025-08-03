package de.mirkosertic.metair.ir.test;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;

import java.lang.reflect.Method;

class ClassTestDescriptor extends AbstractTestDescriptor {

    private final Class<?> testClass;

    public ClassTestDescriptor(final Class<?> testClass, final TestDescriptor parent) {
        super( //
                parent.getUniqueId().append("class", testClass.getName()), //
                testClass.getSimpleName(), //
                ClassSource.from(testClass) //
        );
        this.testClass = testClass;
        setParent(parent);
        addAllChildren();
    }

    private void addAllChildren() {
        for (final Method m : testClass.getMethods()) {
            if (!"<init>".equals(m.getName()) && m.getDeclaringClass() == testClass) {
                // We found a candidate
                addChild(new MethodTestDescriptor(getTestClass(), m, this));
            }
        }
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public Class<?> getTestClass() {
        return testClass;
    }
}
