package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.DominatorTree;
import de.mirkosertic.metair.ir.MethodAnalyzer;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetaIRTestExecutor {

    public void execute(final ExecutionRequest request, final TestDescriptor descriptor) {
        if (descriptor instanceof EngineDescriptor) {
            executeContainer(request, descriptor);
        }

        if (descriptor instanceof ClassTestDescriptor) {
            executeContainer(request, descriptor);
        }

        if (descriptor instanceof final MethodTestDescriptor desc) {
            executeMethod(request, desc);
        }
    }

    private void executeContainer(final ExecutionRequest request, final TestDescriptor containerDescriptor) {
        request.getEngineExecutionListener().executionStarted(containerDescriptor);
        containerDescriptor.getChildren()
                .forEach(descriptor -> execute(request, descriptor));
        request.getEngineExecutionListener().executionFinished(containerDescriptor, TestExecutionResult.successful());
    }

    private void executeMethod(final ExecutionRequest request, final MethodTestDescriptor descriptor) {
        request.getEngineExecutionListener().executionStarted(descriptor);
        try {
            final Class<?> origin = descriptor.getTestClass();

            final URL resource = origin.getClassLoader().getResource(origin.getName().replace('.', File.separatorChar) + ".class");

            try (final InputStream inputStream = resource.openStream()) {
                final byte[] data = inputStream.readAllBytes();

                final ClassFile cf = ClassFile.of();
                final ClassModel model = cf.parse(data);

                for (final MethodModel method : model.methods()) {

                    if (method.methodName().stringValue().equals(descriptor.getTestMethod().getName())) {
                        // We found our candidate
                        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method);

                        final Path targetDir = request.getOutputDirectoryProvider().createOutputDirectory(descriptor);

                        DOTExporter.writeTo(analyzer.ir(), new PrintStream(Files.newOutputStream(targetDir.resolve("ir.dot"))));

                        try (final PrintStream ps = new PrintStream(Files.newOutputStream(targetDir.resolve("bytecode.yaml")))) {
                            ps.print(method.toDebugString());
                        }

                        final DominatorTree dominatorTree = new DominatorTree(analyzer.ir());

                        DOTExporter.writeTo(dominatorTree, new PrintStream(Files.newOutputStream(targetDir.resolve("ir_dominatortree.dot"))));
                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException("Failed to load class data for " + origin.getName(), e);
            }

            request.getEngineExecutionListener().executionFinished(descriptor, TestExecutionResult.successful());

        } catch (final Exception e) {
            request.getEngineExecutionListener().executionFinished(descriptor, TestExecutionResult.failed(e));
        }
    }
}
