package de.mirkosertic.metair.ir.test;

import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.net.URL;
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
            if (resource == null) {
                throw new IllegalStateException("Cannot find class file for " + origin.getName());
            }

            try (final InputStream inputStream = resource.openStream()) {
                final byte[] data = inputStream.readAllBytes();

                final ClassFile cf = ClassFile.of();
                final ClassModel model = cf.parse(data);

                for (final MethodModel method : model.methods()) {

                    if (method.methodName().stringValue().equals(descriptor.getMethodName())) {

                        final Path targetDir = request.getOutputDirectoryProvider().createOutputDirectory(descriptor);

                        new MetaIRTestHelper(targetDir).analyzeAndReport(model, method);
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
