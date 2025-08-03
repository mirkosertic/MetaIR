package de.mirkosertic.metair.ir.test;

import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.net.URI;
import java.util.function.Predicate;

public class MetaIRTestEngine implements TestEngine {

    private static final Predicate<Class<?>> IS_TEST_CONTAINER
            = classCandidate -> AnnotationSupport.isAnnotated(classCandidate, MetaIRTest.class);


    @Override
    public String getId() {
        return "metair-test";
    }


    @Override
    public TestDescriptor discover(final EngineDiscoveryRequest request, final UniqueId uniqueId) {
        final TestDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "MetaIR Test");

        request.getSelectorsByType(ClasspathRootSelector.class).forEach(selector -> appendTestsInClasspathRoot(selector.getClasspathRoot(), engineDescriptor));

        request.getSelectorsByType(PackageSelector.class).forEach(selector -> appendTestsInPackage(selector.getPackageName(), engineDescriptor));

        request.getSelectorsByType(ClassSelector.class).forEach(selector -> appendTestsInClass(selector.getJavaClass(), engineDescriptor));

        request.getSelectorsByType(MethodSelector.class).forEach(selector -> appendMethods(selector, engineDescriptor));

        return engineDescriptor;
    }

    private void appendMethods(final MethodSelector selector, final TestDescriptor engineDescriptor) {
        ReflectionSupport.tryToLoadClass(selector.getClassName()).ifSuccess(clazz -> ReflectionUtils.findMethod(clazz, selector.getMethodName()).ifPresent(method -> engineDescriptor.addChild(new MethodTestDescriptor(clazz, method, engineDescriptor))));
    }

    private void appendTestsInClasspathRoot(final URI uri, final TestDescriptor engineDescriptor) {
        ReflectionSupport.findAllClassesInClasspathRoot(uri, IS_TEST_CONTAINER, name -> true) //
                .stream() //
                .map(aClass -> new ClassTestDescriptor(aClass, engineDescriptor)) //
                .forEach(engineDescriptor::addChild);
    }

    private void appendTestsInPackage(final String packageName, final TestDescriptor engineDescriptor) {
        ReflectionSupport.findAllClassesInPackage(packageName, IS_TEST_CONTAINER, name -> true) //
                .stream() //
                .map(aClass -> new ClassTestDescriptor(aClass, engineDescriptor)) //
                .forEach(engineDescriptor::addChild);
    }

    private void appendTestsInClass(final Class<?> javaClass, final TestDescriptor engineDescriptor) {
        if (AnnotationSupport.isAnnotated(javaClass, MetaIRTest.class)) {
            engineDescriptor.addChild(new ClassTestDescriptor(javaClass, engineDescriptor));
        }
    }

    @Override
    public void execute(final ExecutionRequest request) {
        final TestDescriptor root = request.getRootTestDescriptor();

        new MetaIRTestExecutor().execute(request, root);
    }
}
