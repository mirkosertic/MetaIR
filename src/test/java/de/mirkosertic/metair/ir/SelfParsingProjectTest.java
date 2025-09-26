package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.support.ReflectionSupport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SelfParsingProjectTest {

    @Test
    public void testSingleMethod1() {

        final URL resource = MethodAnalyzer.class.getClassLoader().getResource("de.mirkosertic.metair.ir.MethodAnalyzer$1".replace('.', File.separatorChar) + ".class");
        if (resource == null) {
            throw new IllegalStateException("Cannot find class file");
        }

        try (final InputStream inputStream = resource.openStream()) {
            final byte[] data = inputStream.readAllBytes();

            final ClassFile cf = ClassFile.of();
            final ClassModel model = cf.parse(data);

            for (final MethodModel method : model.methods()) {
                if ("<clinit>".equals(method.methodName().stringValue())) {
                    try {
                        final ResolverContext resolverContext = new ResolverContext();
                        final MethodAnalyzer analyzer = new MethodAnalyzer(resolverContext, IRType.MetaClass.of(model.thisClass().asSymbol()), method);
                    } catch (final IllegalParsingStateException e) {
                        final MethodAnalyzer analyzer = e.getAnalyzer();
                        System.out.println("Failed with testing method " + method.methodName() + " in class");
                        System.out.println(method.toDebugString());

                        System.out.println();

                        DOTExporter.writeBytecodeCFGTo(analyzer, System.out);

                        throw e;
                    } catch (final RuntimeException e) {
                        System.out.println("Failed with testing method " + method.methodName() + " in class");
                        System.out.println(method.toDebugString());
                        throw e;
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load class data", e);
        }

    }

    @TestFactory
    public List<DynamicContainer> testAllProjectClasses() {

        final ResolverContext resolverContext = new ResolverContext();

        return ReflectionSupport.findAllClassesInPackage("de.mirkosertic", _ -> true, s -> true).stream().map(aClass -> {

            final List<DynamicTest> tests = new ArrayList<>();

            try {
                final ResolvedClass resolvedClass = resolverContext.resolveClass(aClass.getName());

                final ClassModel model = resolvedClass.classModel();

                for (final MethodModel method : model.methods()) {

                    tests.add(DynamicTest.dynamicTest(method.methodName().stringValue() + " " + method.methodType().stringValue(), () -> {
                        try {
                            final ResolvedMethod resolvedMethod = resolvedClass.resolveMethod(method);
                            final MethodAnalyzer analyzer = resolvedMethod.analyze();
                        } catch (final IllegalParsingStateException e) {
                            final MethodAnalyzer analyzer = e.getAnalyzer();
                            System.out.println("Failed with testing method " + method.methodName() + " in class " + aClass.getName());
                            System.out.println(method.toDebugString());

                            System.out.println();

                            DOTExporter.writeBytecodeCFGTo(analyzer, System.out);

                            throw e;
                        } catch (final RuntimeException e) {
                            System.out.println("Failed with testing method " + method.methodName() + " in class " + aClass.getName());
                            System.out.println(method.toDebugString());
                            throw e;
                        }
                    }));
                }

            } catch (final Exception e) {
                throw new RuntimeException("Failed to load class data for " + aClass.getName(), e);
            }

            return DynamicContainer.dynamicContainer(aClass.getName(), tests);
        }).toList();
    }
}
