package de.mirkosertic.metair.ir.test;

import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;

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
        final Optional<MetaIRTest> annotation = AnnotationUtils.findAnnotation(testClass, MetaIRTest.class);
        for (final Method m : testClass.getDeclaredMethods()) {
            addChild(new MethodTestDescriptor(getTestClass(), m.getName(), this));
        }
        if (annotation.isPresent() && annotation.get().includeConstructors()) {
            //noinspection rawtypes
            for (final Constructor c : testClass.getDeclaredConstructors()) {
                addChild(new MethodTestDescriptor(getTestClass(), "<init>", this));
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
