package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestExecutor;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.net.URL;

public class SelfParsingProjectTest {

    @Test
    public void testSingleMethod() {

        Class aClass = MetaIRTestExecutor.class;

        final URL resource = aClass.getClassLoader().getResource(aClass.getName().replace('.', File.separatorChar) + ".class");
        if (resource == null) {
            throw new IllegalStateException("Cannot find class file for " + aClass.getName());
        }

        try (final InputStream inputStream = resource.openStream()) {
            final byte[] data = inputStream.readAllBytes();

            final ClassFile cf = ClassFile.of();
            final ClassModel model = cf.parse(data);

            for (final MethodModel method : model.methods()) {
                if ("executeMethod".equals(method.methodName().stringValue())) {
                    try {
                        //final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method);
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
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load class data for " + aClass.getName(), e);
        }

    }

    @Test
    public void testAllProjectClasses() {
        ReflectionSupport.findAllClassesInPackage("de.mirkosertic", _ -> true, s -> !s.endsWith("Test")).forEach(aClass -> {

            final URL resource = aClass.getClassLoader().getResource(aClass.getName().replace('.', File.separatorChar) + ".class");
            if (resource == null) {
                throw new IllegalStateException("Cannot find class file for " + aClass.getName());
            }

            try (final InputStream inputStream = resource.openStream()) {
                final byte[] data = inputStream.readAllBytes();

                final ClassFile cf = ClassFile.of();
                final ClassModel model = cf.parse(data);

                for (final MethodModel method : model.methods()) {
                    try {
                        //final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method);
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
                }
            } catch (final IOException e) {
                throw new RuntimeException("Failed to load class data for " + aClass.getName(), e);
            }
        });
    }
}
